package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CleaningHistoryEntity
import com.example.data.local.HistoryRepository
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.system.IntentLaunchOutcome
import com.example.data.system.StorageRepository
import com.example.data.system.SystemCompatibilityInspector
import com.example.model.AppFilterMode
import com.example.model.AppSortMode
import com.example.model.CleanQueueItem
import com.example.model.CleanTab
import com.example.model.CompatibilityReport
import com.example.model.DeviceStorageInfo
import com.example.model.InstalledAppInfo
import com.example.model.PendingSystemVerification
import com.example.model.QueueAction
import com.example.model.QueueItemStatus
import com.example.model.VerificationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CleanMasterUiState(
    val isScanning: Boolean = true,
    val deviceStorage: DeviceStorageInfo? = null,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val filterMode: AppFilterMode = AppFilterMode.ALL,
    val sortMode: AppSortMode = AppSortMode.NAME_ASC,
    val activeCleanTab: CleanTab = CleanTab.CACHE,
    val cacheSessionPackages: List<String> = emptyList(),
    val cacheSessionIndex: Int = 0,
    val clearDataPackages: Set<String> = emptySet(),
    val clearDataPendingConfirmApps: List<InstalledAppInfo> = emptyList(),
    val multiQueue: List<CleanQueueItem> = emptyList(),
    val isQueueRunning: Boolean = false,
    val pendingVerification: PendingSystemVerification? = null,
    val compatibilityReport: CompatibilityReport? = null,
    val statusBannerMessage: String? = null,
    val inspectAppPackage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val storageRepository = StorageRepository(appContext)
    private val preferencesRepository = UserPreferencesRepository(appContext)
    private val historyRepository = HistoryRepository(
        AppDatabase.getInstance(appContext).cleaningHistoryDao()
    )

    private val _uiState = MutableStateFlow(CleanMasterUiState())
    val uiState: StateFlow<CleanMasterUiState> = _uiState.asStateFlow()

    val isDarkTheme: StateFlow<Boolean> = preferencesRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val confirmClearData: StateFlow<Boolean> = preferencesRepository.confirmClearData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val historyEntries: StateFlow<List<CleaningHistoryEntity>> = historyRepository.historyEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredApps: StateFlow<List<InstalledAppInfo>> = _uiState
        .map { state ->
            applyFilterAndSort(
                apps = state.installedApps,
                query = state.searchQuery,
                filter = state.filterMode,
                sort = state.sortMode
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refreshAll(initial = true)
    }

    fun refreshAll(initial: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val storage = storageRepository.getDeviceStorageInfo()
            val apps = storageRepository.scanInstalledApps()
            val report = SystemCompatibilityInspector.buildCompatibilityReport(appContext)
            _uiState.update { current ->
                val validPackages = apps.map { it.packageName }.toSet()
                val retainedSelection = current.selectedPackages.intersect(validPackages)
                val retainedClearData = current.clearDataPackages.intersect(validPackages)
                current.copy(
                    isScanning = false,
                    deviceStorage = storage,
                    installedApps = apps,
                    selectedPackages = retainedSelection,
                    clearDataPackages = retainedClearData,
                    compatibilityReport = report,
                    statusBannerMessage = if (initial) null else current.statusBannerMessage
                )
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            val prevPermission = _uiState.value.compatibilityReport?.hasUsageAccessPermission ?: false
            val newReport = SystemCompatibilityInspector.buildCompatibilityReport(appContext)
            val storage = storageRepository.getDeviceStorageInfo()
            _uiState.update { it.copy(compatibilityReport = newReport, deviceStorage = storage) }

            if (!prevPermission && newReport.hasUsageAccessPermission) {
                refreshAll()
            }

            val pending = _uiState.value.pendingVerification
            if (pending != null) {
                verifyPendingSystemOperation(pending)
            }
        }
    }

    private suspend fun verifyPendingSystemOperation(pending: PendingSystemVerification) {
        val currentStats = storageRepository.querySingleAppStats(pending.packageName)
        val beforeCache = pending.initialCacheBytes
        val beforeData = pending.initialDataBytes
        val afterCache = currentStats.cacheBytes
        val afterData = currentStats.dataBytes

        val verifiedFreedBytes: Long? = when (pending.action) {
            QueueAction.CLEAR_CACHE -> {
                if (beforeCache != null && afterCache != null && afterCache < beforeCache) {
                    beforeCache - afterCache
                } else null
            }
            QueueAction.CLEAR_DATA -> {
                if (beforeData != null && afterData != null && afterData < beforeData) {
                    beforeData - afterData
                } else null
            }
            QueueAction.SKIP -> null
        }

        val isVerifiedCompleted = verifiedFreedBytes != null && verifiedFreedBytes > 0L

        if (isVerifiedCompleted) {
            val opLabel = if (pending.action == QueueAction.CLEAR_DATA) {
                "Borrado de datos"
            } else {
                "Limpieza de caché"
            }
            historyRepository.recordOperation(
                appName = pending.appName,
                packageName = pending.packageName,
                operationType = opLabel,
                status = QueueItemStatus.COMPLETED.label,
                recoveredBytes = verifiedFreedBytes,
                details = "Verificado por StorageStatsManager tras confirmación en Ajustes del sistema."
            )

            _uiState.update { state ->
                val updatedQueue = state.multiQueue.map { item ->
                    if (item.packageName == pending.packageName && item.status == QueueItemStatus.REQUIRES_CONFIRMATION) {
                        item.copy(
                            status = QueueItemStatus.COMPLETED,
                            recoveredBytes = verifiedFreedBytes,
                            statusNote = "Reducción verificada por StorageStatsManager."
                        )
                    } else item
                }
                state.copy(
                    multiQueue = updatedQueue,
                    pendingVerification = null,
                    statusBannerMessage = "Verificado: ${pending.appName} liberó espacio correctamente."
                )
            }
            refreshAll()

            if (pending.mode == VerificationMode.CACHE_ASSISTANT) {
                advanceCacheSessionIfPossible()
            } else if (pending.mode == VerificationMode.MULTI_QUEUE && _uiState.value.isQueueRunning) {
                processNextQueueStep()
            }
        } else {
            // Do NOT mark as Completed if not verified!
            val reason = if (SystemCompatibilityInspector.hasUsageStatsPermission(appContext)) {
                "No se detectó reducción de tamaño aún. Confirma la acción en Ajustes de Android o márcala manualmente como no realizada."
            } else {
                "Sin acceso a estadísticas de uso para verificar el cambio automáticamente. Puedes reabrir Ajustes o continuar con la siguiente."
            }
            _uiState.update { state ->
                val updatedQueue = state.multiQueue.map { item ->
                    if (item.packageName == pending.packageName) {
                        item.copy(
                            status = QueueItemStatus.REQUIRES_CONFIRMATION,
                            statusNote = reason
                        )
                    } else item
                }
                state.copy(
                    multiQueue = updatedQueue,
                    statusBannerMessage = "${pending.appName}: $reason"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilterMode(mode: AppFilterMode) {
        _uiState.update { it.copy(filterMode = mode) }
    }

    fun updateSortMode(mode: AppSortMode) {
        _uiState.update { it.copy(sortMode = mode) }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val next = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = next)
        }
    }

    fun selectAllFilteredApps() {
        val visiblePackages = filteredApps.value.map { it.packageName }.toSet()
        _uiState.update { state ->
            state.copy(selectedPackages = state.selectedPackages + visiblePackages)
        }
    }

    fun deselectAllApps() {
        _uiState.update { it.copy(selectedPackages = emptySet()) }
    }

    fun setInspectAppPackage(packageName: String?) {
        _uiState.update { it.copy(inspectAppPackage = packageName) }
    }

    fun setActiveCleanTab(tab: CleanTab) {
        _uiState.update { it.copy(activeCleanTab = tab) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    // --- Cache Manager (Requirement 4) ---

    fun startCacheSessionFromSelection() {
        val selected = _uiState.value.selectedPackages.toList()
        if (selected.isEmpty()) {
            _uiState.update {
                it.copy(statusBannerMessage = "Selecciona al menos una aplicación para limpiar caché.")
            }
            return
        }
        _uiState.update {
            it.copy(
                activeCleanTab = CleanTab.CACHE,
                cacheSessionPackages = selected,
                cacheSessionIndex = 0,
                statusBannerMessage = "Sesión de limpieza de caché preparada (${selected.size} apps)."
            )
        }
    }

    fun executeCacheStepForApp(app: InstalledAppInfo) {
        viewModelScope.launch {
            if (app.isSelfApp) {
                val res = storageRepository.clearOwnAppCache()
                historyRepository.recordOperation(
                    appName = app.appName,
                    packageName = app.packageName,
                    operationType = "Limpieza de caché propia",
                    status = QueueItemStatus.COMPLETED.label,
                    recoveredBytes = res.freedBytes,
                    details = "Caché interna de Clean Master AI eliminada directamente."
                )
                _uiState.update {
                    it.copy(
                        statusBannerMessage = "Caché interna limpiada (${com.example.util.ByteFormatter.formatBytes(res.freedBytes)} recuperados)."
                    )
                }
                refreshAll()
                advanceCacheSessionIfPossible()
            } else {
                val outcome = SystemCompatibilityInspector.openAppDetailsSettings(appContext, app.packageName)
                when (outcome) {
                    IntentLaunchOutcome.OpenedAppDetails,
                    IntentLaunchOutcome.OpenedManageAppsFallback -> {
                        _uiState.update {
                            it.copy(
                                pendingVerification = PendingSystemVerification(
                                    mode = VerificationMode.CACHE_ASSISTANT,
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    action = QueueAction.CLEAR_CACHE,
                                    initialCacheBytes = app.cacheBytes,
                                    initialDataBytes = app.dataBytes
                                ),
                                statusBannerMessage = "Se abrió la ficha oficial de ${app.appName}. Confirma «Borrar caché» y vuelve a Clean Master AI."
                            )
                        }
                    }
                    is IntentLaunchOutcome.Failed -> {
                        historyRepository.recordOperation(
                            appName = app.appName,
                            packageName = app.packageName,
                            operationType = "Limpieza de caché",
                            status = QueueItemStatus.ERROR.label,
                            recoveredBytes = null,
                            details = outcome.message
                        )
                        _uiState.update { it.copy(statusBannerMessage = outcome.message) }
                    }
                }
            }
        }
    }

    fun skipCurrentCacheSessionApp() {
        val state = _uiState.value
        val pkg = state.cacheSessionPackages.getOrNull(state.cacheSessionIndex)
        if (pkg != null) {
            val appName = state.installedApps.find { it.packageName == pkg }?.appName ?: pkg
            viewModelScope.launch {
                historyRepository.recordOperation(
                    appName = appName,
                    packageName = pkg,
                    operationType = "Limpieza de caché",
                    status = QueueItemStatus.REQUIRES_CONFIRMATION.label,
                    recoveredBytes = null,
                    details = "Paso omitido o sin verificación automática confirmada."
                )
            }
        }
        _uiState.update { it.copy(pendingVerification = null) }
        advanceCacheSessionIfPossible()
    }

    private fun advanceCacheSessionIfPossible() {
        _uiState.update { state ->
            if (state.cacheSessionPackages.isEmpty()) return@update state
            val nextIdx = state.cacheSessionIndex + 1
            if (nextIdx >= state.cacheSessionPackages.size) {
                state.copy(
                    cacheSessionPackages = emptyList(),
                    cacheSessionIndex = 0,
                    statusBannerMessage = "Recorrido guiado de caché finalizado."
                )
            } else {
                state.copy(cacheSessionIndex = nextIdx)
            }
        }
    }

    // --- Clear Data Manager (Requirement 5) ---

    fun toggleClearDataSelection(packageName: String) {
        _uiState.update { state ->
            val next = if (packageName in state.clearDataPackages) {
                state.clearDataPackages - packageName
            } else {
                state.clearDataPackages + packageName
            }
            state.copy(clearDataPackages = next)
        }
    }

    fun requestClearDataForApps(apps: List<InstalledAppInfo>) {
        if (apps.isEmpty()) return
        // Always require explicit confirmation for Clear Data per Requirement 5
        _uiState.update { it.copy(clearDataPendingConfirmApps = apps) }
    }

    fun dismissClearDataConfirmation() {
        _uiState.update { it.copy(clearDataPendingConfirmApps = emptyList()) }
    }

    fun confirmAndLaunchClearData(app: InstalledAppInfo) {
        _uiState.update { it.copy(clearDataPendingConfirmApps = emptyList()) }
        viewModelScope.launch {
            val outcome = SystemCompatibilityInspector.openAppDetailsSettings(appContext, app.packageName)
            when (outcome) {
                IntentLaunchOutcome.OpenedAppDetails,
                IntentLaunchOutcome.OpenedManageAppsFallback -> {
                    historyRepository.recordOperation(
                        appName = app.appName,
                        packageName = app.packageName,
                        operationType = "Borrado de datos (Ajustes)",
                        status = QueueItemStatus.REQUIRES_CONFIRMATION.label,
                        recoveredBytes = null,
                        details = "Pantalla oficial abierta tras confirmación explícita del usuario."
                    )
                    _uiState.update {
                        it.copy(
                            pendingVerification = PendingSystemVerification(
                                mode = VerificationMode.CLEAR_DATA_ASSISTANT,
                                packageName = app.packageName,
                                appName = app.appName,
                                action = QueueAction.CLEAR_DATA,
                                initialCacheBytes = app.cacheBytes,
                                initialDataBytes = app.dataBytes
                            ),
                            statusBannerMessage = "Se abrió ${app.appName} en Ajustes de Android para borrar datos bajo tu confirmación."
                        )
                    }
                }
                is IntentLaunchOutcome.Failed -> {
                    historyRepository.recordOperation(
                        appName = app.appName,
                        packageName = app.packageName,
                        operationType = "Borrado de datos",
                        status = QueueItemStatus.ERROR.label,
                        recoveredBytes = null,
                        details = outcome.message
                    )
                    _uiState.update { it.copy(statusBannerMessage = outcome.message) }
                }
            }
        }
    }

    // --- Multi-Clean Queue (Requirement 6) ---

    fun syncSelectedAppsIntoQueue(defaultAction: QueueAction = QueueAction.CLEAR_CACHE) {
        val state = _uiState.value
        val sourceApps = if (state.selectedPackages.isNotEmpty()) {
            state.installedApps.filter { it.packageName in state.selectedPackages }
        } else {
            emptyList()
        }
        if (sourceApps.isEmpty()) {
            _uiState.update {
                it.copy(statusBannerMessage = "Primero selecciona aplicaciones en el Administrador de Apps.")
            }
            return
        }
        val existingMap = state.multiQueue.associateBy { it.packageName }
        val newQueue = sourceApps.map { app ->
            existingMap[app.packageName]?.copy(
                initialCacheBytes = app.cacheBytes,
                initialDataBytes = app.dataBytes
            ) ?: CleanQueueItem(
                packageName = app.packageName,
                appName = app.appName,
                isSelfApp = app.isSelfApp,
                action = defaultAction,
                status = QueueItemStatus.PENDING,
                initialCacheBytes = app.cacheBytes,
                initialDataBytes = app.dataBytes
            )
        }
        _uiState.update {
            it.copy(
                multiQueue = newQueue,
                activeCleanTab = CleanTab.MULTI_QUEUE,
                isQueueRunning = false
            )
        }
    }

    fun addOrUpdateAppInQueue(app: InstalledAppInfo, action: QueueAction) {
        _uiState.update { state ->
            val mutable = state.multiQueue.toMutableList()
            val idx = mutable.indexOfFirst { it.packageName == app.packageName }
            val newItem = CleanQueueItem(
                packageName = app.packageName,
                appName = app.appName,
                isSelfApp = app.isSelfApp,
                action = action,
                status = QueueItemStatus.PENDING,
                initialCacheBytes = app.cacheBytes,
                initialDataBytes = app.dataBytes
            )
            if (idx >= 0) mutable[idx] = newItem else mutable.add(newItem)
            state.copy(
                multiQueue = mutable,
                statusBannerMessage = "${app.appName} añadida a la cola (${action.label})."
            )
        }
    }

    fun updateQueueItemAction(packageName: String, action: QueueAction) {
        _uiState.update { state ->
            state.copy(
                multiQueue = state.multiQueue.map { item ->
                    if (item.packageName == packageName) {
                        item.copy(
                            action = action,
                            status = QueueItemStatus.PENDING,
                            recoveredBytes = null,
                            statusNote = ""
                        )
                    } else item
                }
            )
        }
    }

    fun removeQueueItem(packageName: String) {
        _uiState.update { state ->
            state.copy(multiQueue = state.multiQueue.filterNot { it.packageName == packageName })
        }
    }

    fun resetQueueStatuses() {
        _uiState.update { state ->
            state.copy(
                isQueueRunning = false,
                pendingVerification = null,
                multiQueue = state.multiQueue.map {
                    it.copy(status = QueueItemStatus.PENDING, recoveredBytes = null, statusNote = "")
                }
            )
        }
    }

    fun startMultiCleanQueue() {
        val queue = _uiState.value.multiQueue
        if (queue.isEmpty()) {
            _uiState.update {
                it.copy(statusBannerMessage = "La cola está vacía. Añade aplicaciones primero.")
            }
            return
        }
        _uiState.update { it.copy(isQueueRunning = true) }
        viewModelScope.launch {
            processNextQueueStep()
        }
    }

    fun continueQueueAfterManualStep(markCurrentNotVerified: Boolean) {
        val pending = _uiState.value.pendingVerification
        if (pending != null && markCurrentNotVerified) {
            viewModelScope.launch {
                historyRepository.recordOperation(
                    appName = pending.appName,
                    packageName = pending.packageName,
                    operationType = if (pending.action == QueueAction.CLEAR_DATA) "Borrado de datos" else "Limpieza de caché",
                    status = QueueItemStatus.REQUIRES_CONFIRMATION.label,
                    recoveredBytes = null,
                    details = "No verificable automáticamente; usuario continuó con el siguiente elemento."
                )
            }
            _uiState.update { state ->
                state.copy(
                    pendingVerification = null,
                    multiQueue = state.multiQueue.map { item ->
                        if (item.packageName == pending.packageName &&
                            item.status == QueueItemStatus.REQUIRES_CONFIRMATION
                        ) {
                            item.copy(
                                status = QueueItemStatus.NOT_ALLOWED,
                                statusNote = "Sin verificación de cambio o cancelado por el usuario."
                            )
                        } else item
                    }
                )
            }
        }
        viewModelScope.launch {
            processNextQueueStep()
        }
    }

    private suspend fun processNextQueueStep() {
        val state = _uiState.value
        val nextItem = state.multiQueue.firstOrNull { it.status == QueueItemStatus.PENDING }
        if (nextItem == null) {
            _uiState.update {
                it.copy(
                    isQueueRunning = false,
                    pendingVerification = null,
                    statusBannerMessage = "Cola de limpieza procesada."
                )
            }
            return
        }

        if (nextItem.action == QueueAction.SKIP) {
            updateQueueItem(
                nextItem.packageName,
                status = QueueItemStatus.NOT_ALLOWED,
                note = "Marcada como «No modificar» por el usuario."
            )
            processNextQueueStep()
            return
        }

        updateQueueItem(nextItem.packageName, status = QueueItemStatus.IN_PROGRESS, note = "Procesando…")

        if (nextItem.isSelfApp && nextItem.action == QueueAction.CLEAR_CACHE) {
            val result = storageRepository.clearOwnAppCache()
            historyRepository.recordOperation(
                appName = nextItem.appName,
                packageName = nextItem.packageName,
                operationType = "Limpieza de caché propia",
                status = QueueItemStatus.COMPLETED.label,
                recoveredBytes = result.freedBytes,
                details = "Operación automática autorizada en caché interna."
            )
            _uiState.update { s ->
                s.copy(
                    multiQueue = s.multiQueue.map {
                        if (it.packageName == nextItem.packageName) {
                            it.copy(
                                status = QueueItemStatus.COMPLETED,
                                recoveredBytes = result.freedBytes,
                                statusNote = "Caché propia limpiada y verificada."
                            )
                        } else it
                    }
                )
            }
            refreshAll()
            processNextQueueStep()
            return
        }

        // External app or Clear Data requires official Android Settings confirmation
        val outcome = SystemCompatibilityInspector.openAppDetailsSettings(appContext, nextItem.packageName)
        when (outcome) {
            IntentLaunchOutcome.OpenedAppDetails,
            IntentLaunchOutcome.OpenedManageAppsFallback -> {
                val note = if (nextItem.action == QueueAction.CLEAR_DATA) {
                    "Se abrió Ajustes para borrar datos. Confirma manualmente y vuelve."
                } else {
                    "Android requiere confirmación manual para limpiar caché de terceros. Vuelve al terminar."
                }
                _uiState.update { s ->
                    s.copy(
                        multiQueue = s.multiQueue.map {
                            if (it.packageName == nextItem.packageName) {
                                it.copy(
                                    status = QueueItemStatus.REQUIRES_CONFIRMATION,
                                    statusNote = note
                                )
                            } else it
                        },
                        pendingVerification = PendingSystemVerification(
                            mode = VerificationMode.MULTI_QUEUE,
                            packageName = nextItem.packageName,
                            appName = nextItem.appName,
                            action = nextItem.action,
                            initialCacheBytes = nextItem.initialCacheBytes,
                            initialDataBytes = nextItem.initialDataBytes
                        ),
                        statusBannerMessage = "${nextItem.appName}: $note"
                    )
                }
            }
            is IntentLaunchOutcome.Failed -> {
                historyRepository.recordOperation(
                    appName = nextItem.appName,
                    packageName = nextItem.packageName,
                    operationType = nextItem.action.label,
                    status = QueueItemStatus.ERROR.label,
                    recoveredBytes = null,
                    details = outcome.message
                )
                updateQueueItem(nextItem.packageName, status = QueueItemStatus.ERROR, note = outcome.message)
                processNextQueueStep()
            }
        }
    }

    private fun updateQueueItem(packageName: String, status: QueueItemStatus, note: String) {
        _uiState.update { s ->
            s.copy(
                multiQueue = s.multiQueue.map {
                    if (it.packageName == packageName) it.copy(status = status, statusNote = note) else it
                }
            )
        }
    }

    // --- Settings & History ---

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkTheme(enabled) }
    }

    fun setConfirmClearData(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setConfirmClearData(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _uiState.update { it.copy(statusBannerMessage = "Historial local eliminado.") }
        }
    }

    fun openUsageAccessSettings() {
        val launched = SystemCompatibilityInspector.openUsageStatsSettings(appContext)
        if (!launched) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Este dispositivo no permitió abrir Acceso a datos de uso directamente. Búscalo en Ajustes → Aplicaciones → Acceso especial."
                )
            }
        }
    }

    fun openSelfAppSettings() {
        val outcome = SystemCompatibilityInspector.openSelfAppSettings(appContext)
        if (outcome is IntentLaunchOutcome.Failed) {
            _uiState.update { it.copy(statusBannerMessage = outcome.message) }
        }
    }

    private fun applyFilterAndSort(
        apps: List<InstalledAppInfo>,
        query: String,
        filter: AppFilterMode,
        sort: AppSortMode
    ): List<InstalledAppInfo> {
        val q = query.trim().lowercase()
        val filtered = apps.filter { app ->
            val matchesQuery = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
            val matchesFilter = when (filter) {
                AppFilterMode.ALL -> true
                AppFilterMode.USER_APPS -> !app.isSystemApp
                AppFilterMode.SYSTEM_APPS -> app.isSystemApp
                AppFilterMode.WITH_CACHE -> (app.cacheBytes ?: 0L) > 0L
            }
            matchesQuery && matchesFilter
        }
        return when (sort) {
            AppSortMode.NAME_ASC -> filtered.sortedBy { it.appName.lowercase() }
            AppSortMode.TOTAL_DESC -> filtered.sortedByDescending { it.totalBytes ?: it.apkSizeBytes ?: -1L }
            AppSortMode.CACHE_DESC -> filtered.sortedByDescending { it.cacheBytes ?: -1L }
        }
    }
}
