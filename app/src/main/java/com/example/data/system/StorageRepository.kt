package com.example.data.system

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import com.example.model.DeviceStorageInfo
import com.example.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppStorageMeasurement(
    val appBytes: Long?,
    val dataBytes: Long?,
    val cacheBytes: Long?,
    val totalBytes: Long?
)

data class SelfCacheClearResult(
    val beforeBytes: Long,
    val afterBytes: Long,
    val freedBytes: Long
)

class StorageRepository(private val context: Context) {

    suspend fun getDeviceStorageInfo(): DeviceStorageInfo = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                if (statsManager != null) {
                    val total = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                    val free = statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                    val used = (total - free).coerceAtLeast(0L)
                    if (total > 0L) {
                        return@withContext DeviceStorageInfo(
                            totalBytes = total,
                            usedBytes = used,
                            freeBytes = free,
                            sourceLabel = "StorageStatsManager (API ${Build.VERSION.SDK_INT})"
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallback safely to StatFs if OEM blocks UUID_DEFAULT query
            }
        }

        val dataDir = Environment.getDataDirectory()
        val statFs = StatFs(dataDir.path)
        val total = statFs.totalBytes
        val free = statFs.availableBytes
        val used = (total - free).coerceAtLeast(0L)
        DeviceStorageInfo(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free,
            sourceLabel = "StatFs (Sistema de archivos interno)"
        )
    }

    suspend fun scanInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val uniquePackages = linkedMapOf<String, ApplicationInfo>()
        for (resolveInfo in resolved) {
            val appInfo = resolveInfo.activityInfo?.applicationInfo ?: continue
            uniquePackages.putIfAbsent(appInfo.packageName, appInfo)
        }

        // Guarantee our own app is always included so its internal cache can be inspected and cleaned directly
        if (!uniquePackages.containsKey(context.packageName)) {
            try {
                val selfInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(context.packageName, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(context.packageName, 0)
                }
                uniquePackages[context.packageName] = selfInfo
            } catch (_: Exception) {
            }
        }

        val canQueryStats = SystemCompatibilityInspector.hasUsageStatsPermission(context)
        val storageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && canQueryStats) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else {
            null
        }

        uniquePackages.values.map { appInfo ->
            val pkgName = appInfo.packageName
            val label = try {
                pm.getApplicationLabel(appInfo).toString().ifBlank { pkgName }
            } catch (_: Exception) {
                pkgName
            }
            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                null
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val apkBytes = try {
                val sourceFile = appInfo.sourceDir?.let(::File)
                if (sourceFile != null && sourceFile.exists() && sourceFile.canRead()) {
                    sourceFile.length()
                } else null
            } catch (_: Exception) {
                null
            }

            val stats = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && storageStatsManager != null) {
                queryPackageStatsInternal(storageStatsManager, appInfo)
            } else {
                AppStorageMeasurement(null, null, null, null)
            }

            InstalledAppInfo(
                packageName = pkgName,
                appName = label,
                icon = icon,
                isSystemApp = isSystem,
                isSelfApp = pkgName == context.packageName,
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                cacheBytes = stats.cacheBytes,
                totalBytes = stats.totalBytes,
                apkSizeBytes = apkBytes
            )
        }.sortedBy { it.appName.lowercase() }
    }

    suspend fun querySingleAppStats(packageName: String): AppStorageMeasurement = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !SystemCompatibilityInspector.hasUsageStatsPermission(context)
        ) {
            return@withContext AppStorageMeasurement(null, null, null, null)
        }
        val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
            ?: return@withContext AppStorageMeasurement(null, null, null, null)
        val pm = context.packageManager
        val appInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        } catch (_: Exception) {
            return@withContext AppStorageMeasurement(null, null, null, null)
        }
        queryPackageStatsInternal(statsManager, appInfo)
    }

    private fun queryPackageStatsInternal(
        statsManager: StorageStatsManager,
        appInfo: ApplicationInfo
    ): AppStorageMeasurement {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return AppStorageMeasurement(null, null, null, null)
        }
        return try {
            val uuid = appInfo.storageUuid
            val stats: StorageStats = statsManager.queryStatsForPackage(
                uuid,
                appInfo.packageName,
                Process.myUserHandle()
            )
            val appBytes = stats.appBytes.coerceAtLeast(0L)
            val dataBytes = stats.dataBytes.coerceAtLeast(0L)
            val cacheBytes = stats.cacheBytes.coerceAtLeast(0L)
            // In Android StorageStats, total size of package is appBytes + dataBytes (dataBytes includes cacheBytes)
            val totalBytes = appBytes + dataBytes
            AppStorageMeasurement(
                appBytes = appBytes,
                dataBytes = dataBytes,
                cacheBytes = cacheBytes,
                totalBytes = totalBytes
            )
        } catch (_: Exception) {
            AppStorageMeasurement(null, null, null, null)
        }
    }

    suspend fun clearOwnAppCache(): SelfCacheClearResult = withContext(Dispatchers.IO) {
        val dirs = listOfNotNull(
            context.cacheDir,
            context.codeCacheDir,
            context.externalCacheDir
        ).distinct()

        val before = dirs.sumOf { directorySize(it) }
        for (dir in dirs) {
            deleteDirectoryContents(dir)
        }
        val after = dirs.sumOf { directorySize(it) }
        SelfCacheClearResult(
            beforeBytes = before,
            afterBytes = after,
            freedBytes = (before - after).coerceAtLeast(0L)
        )
    }

    private fun directorySize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    }

    private fun deleteDirectoryContents(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                deleteDirectoryContents(child)
            }
            child.delete()
        }
    }
}
