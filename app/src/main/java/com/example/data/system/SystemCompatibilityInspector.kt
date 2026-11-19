package com.example.data.system

import android.Manifest
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.model.CompatibilityReport
import java.util.Locale

sealed interface IntentLaunchOutcome {
    data object OpenedAppDetails : IntentLaunchOutcome
    data object OpenedManageAppsFallback : IntentLaunchOutcome
    data class Failed(val message: String) : IntentLaunchOutcome
}

object SystemCompatibilityInspector {

    fun hasUsageStatsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun getOemStoragePathHint(manufacturerRaw: String = Build.MANUFACTURER): String {
        val m = manufacturerRaw.lowercase(Locale.ROOT)
        return when {
            m.contains("samsung") ->
                "Samsung (One UI): en la ficha que se abrirá, toca «Almacenamiento» y luego «Eliminar caché» o «Eliminar datos»."
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ->
                "Xiaomi / Redmi / POCO (MIUI / HyperOS): toca «Almacenamiento» → «Limpiar datos» y elige «Limpiar caché» o «Borrar todos los datos»."
            m.contains("motorola") || m.contains("google") || m.contains("nokia") ->
                "Motorola / Android estándar: entra en «Almacenamiento y caché» y pulsa «Borrar caché» o «Liberar espacio de almacenamiento»."
            m.contains("tecno") || m.contains("infinix") || m.contains("itel") ->
                "Tecno / Infinix (HiOS / XOS): selecciona «Almacenamiento y caché» y confirma la limpieza correspondiente."
            m.contains("zte") || m.contains("nubia") ->
                "ZTE / Nubia (MyOS / RedMagic): abre «Almacenamiento» dentro de la información de la app y confirma la eliminación."
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ->
                "OPPO / realme / OnePlus (ColorOS / OxygenOS): entra en «Uso de almacenamiento» y toca «Borrar caché» o «Borrar datos»."
            m.contains("huawei") || m.contains("honor") ->
                "Huawei / Honor (EMUI / MagicOS): abre «Almacenamiento» y selecciona «Borrar memoria caché» o «Borrar datos»."
            else ->
                "Fabricante ${Build.MANUFACTURER}: abre la sección «Almacenamiento» o «Almacenamiento y caché» en la pantalla oficial del sistema y confirma manualmente."
        }
    }

    fun buildCompatibilityReport(context: Context): CompatibilityReport {
        val sdk = Build.VERSION.SDK_INT
        val supportsStats = sdk >= Build.VERSION_CODES.O
        val hasUsage = hasUsageStatsPermission(context)
        val limitations = buildString {
            append("Android ${Build.VERSION.RELEASE} (API $sdk). ")
            if (supportsStats) {
                append("StorageStatsManager disponible para consultar tamaños de aplicación, datos y caché con acceso a estadísticas de uso. ")
            } else {
                append("Dispositivo anterior a Android 8.0: StorageStatsManager no está disponible; se muestran tamaños alternativos cuando el sistema los expone. ")
            }
            append("Por seguridad de Android (API 26+), ninguna app estándar puede borrar silenciosamente la caché o los datos privados de otras aplicaciones; por ello, Clean Master AI abre la pantalla oficial del sistema para tu confirmación manual.")
        }

        return CompatibilityReport(
            androidVersionLabel = "Android ${Build.VERSION.RELEASE} (API $sdk)",
            sdkInt = sdk,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            supportsStorageStatsManager = supportsStats,
            hasUsageAccessPermission = hasUsage,
            oemSettingsPathHint = getOemStoragePathHint(),
            apiLimitationsSummary = limitations
        )
    }

    fun openAppDetailsSettings(context: Context, packageName: String): IntentLaunchOutcome {
        val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(detailsIntent)
            IntentLaunchOutcome.OpenedAppDetails
        } catch (_: ActivityNotFoundException) {
            try {
                val manageIntent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(manageIntent)
                IntentLaunchOutcome.OpenedManageAppsFallback
            } catch (e: Exception) {
                IntentLaunchOutcome.Failed(
                    e.localizedMessage ?: "No se pudo abrir la configuración de aplicaciones en este dispositivo."
                )
            }
        } catch (e: Exception) {
            IntentLaunchOutcome.Failed(
                e.localizedMessage ?: "Error al abrir la ficha oficial de la aplicación."
            )
        }
    }

    fun openUsageStatsSettings(context: Context): Boolean {
        val packageSpecific = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(packageSpecific)
            true
        } catch (_: Exception) {
            try {
                val generic = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(generic)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openSelfAppSettings(context: Context): IntentLaunchOutcome {
        return openAppDetailsSettings(context, context.packageName)
    }
}
