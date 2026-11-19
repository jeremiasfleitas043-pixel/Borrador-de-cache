package com.example.model

import android.graphics.drawable.Drawable

data class DeviceStorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val sourceLabel: String
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isSelfApp: Boolean,
    val appBytes: Long?,
    val dataBytes: Long?,
    val cacheBytes: Long?,
    val totalBytes: Long?,
    val apkSizeBytes: Long?
)

enum class AppFilterMode(val label: String) {
    ALL("Todas"),
    USER_APPS("Usuario"),
    SYSTEM_APPS("Sistema"),
    WITH_CACHE("Con caché")
}

enum class AppSortMode(val label: String) {
    NAME_ASC("Nombre A-Z"),
    TOTAL_DESC("Mayor tamaño"),
    CACHE_DESC("Mayor caché")
}

enum class QueueAction(val label: String) {
    CLEAR_CACHE("Limpiar caché"),
    CLEAR_DATA("Borrar datos"),
    SKIP("No modificar")
}

enum class QueueItemStatus(val label: String) {
    PENDING("Pendiente"),
    IN_PROGRESS("En proceso"),
    REQUIRES_CONFIRMATION("Requiere confirmación"),
    COMPLETED("Completada"),
    NOT_ALLOWED("No permitida"),
    ERROR("Error")
}

data class CleanQueueItem(
    val packageName: String,
    val appName: String,
    val isSelfApp: Boolean,
    val action: QueueAction,
    val status: QueueItemStatus = QueueItemStatus.PENDING,
    val initialCacheBytes: Long? = null,
    val initialDataBytes: Long? = null,
    val recoveredBytes: Long? = null,
    val statusNote: String = ""
)

enum class CleanTab(val label: String) {
    CACHE("Caché"),
    CLEAR_DATA("BORRAR DATOS"),
    MULTI_QUEUE("Cola múltiple")
}

data class CompatibilityReport(
    val androidVersionLabel: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val brand: String,
    val supportsStorageStatsManager: Boolean,
    val hasUsageAccessPermission: Boolean,
    val oemSettingsPathHint: String,
    val apiLimitationsSummary: String
)

data class PendingSystemVerification(
    val mode: VerificationMode,
    val packageName: String,
    val appName: String,
    val action: QueueAction,
    val initialCacheBytes: Long?,
    val initialDataBytes: Long?
)

enum class VerificationMode {
    CACHE_ASSISTANT,
    CLEAR_DATA_ASSISTANT,
    MULTI_QUEUE
}
