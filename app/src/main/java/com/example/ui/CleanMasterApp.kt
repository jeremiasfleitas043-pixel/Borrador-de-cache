package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.CleanTab
import com.example.ui.components.AppDetailsSheet
import com.example.ui.components.ClearDataConfirmDialog
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.CleanHubScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CleanMasterTheme

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_APPS = "apps"
const val ROUTE_CLEAN = "clean"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val destinations = listOf(
    NavDestination(ROUTE_DASHBOARD, "Inicio", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    NavDestination(ROUTE_APPS, "Apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    NavDestination(ROUTE_CLEAN, "Limpieza", Icons.Rounded.CleaningServices, Icons.Outlined.CleaningServices),
    NavDestination(ROUTE_HISTORY, "Historial", Icons.Rounded.History, Icons.Outlined.History),
    NavDestination(ROUTE_SETTINGS, "Ajustes", Icons.Rounded.Settings, Icons.Outlined.Settings)
)

@Composable
fun CleanMasterApp(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val confirmClearData by viewModel.confirmClearData.collectAsStateWithLifecycle()
    val historyEntries by viewModel.historyEntries.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    CleanMasterTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

        val navigateToRoute: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        if (uiState.clearDataPendingConfirmApps.isNotEmpty()) {
            ClearDataConfirmDialog(
                apps = uiState.clearDataPendingConfirmApps,
                requireExtraAck = confirmClearData,
                onConfirmFirstApp = { app -> viewModel.confirmAndLaunchClearData(app) },
                onDismiss = { viewModel.dismissClearDataConfirmation() }
            )
        }

        val inspectedApp = uiState.inspectAppPackage?.let { pkg ->
            uiState.installedApps.find { it.packageName == pkg }
        }
        if (inspectedApp != null) {
            AppDetailsSheet(
                app = inspectedApp,
                hasUsageStatsPermission = uiState.compatibilityReport?.hasUsageAccessPermission == true,
                onGrantUsageAccess = { viewModel.openUsageAccessSettings() },
                onCleanCache = { app ->
                    viewModel.setInspectAppPackage(null)
                    viewModel.executeCacheStepForApp(app)
                },
                onRequestClearData = { app ->
                    viewModel.setInspectAppPackage(null)
                    viewModel.requestClearDataForApps(listOf(app))
                },
                onAddToQueue = { app, action ->
                    viewModel.addOrUpdateAppInQueue(app, action)
                    viewModel.setInspectAppPackage(null)
                },
                onDismiss = { viewModel.setInspectAppPackage(null) }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_rail_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    uiState = uiState,
                                    historyEntries = historyEntries,
                                    onAnalyzeAppsClick = {
                                        viewModel.refreshAll()
                                        navigateToRoute(ROUTE_APPS)
                                    },
                                    onMultiCleanClick = {
                                        viewModel.setActiveCleanTab(CleanTab.MULTI_QUEUE)
                                        if (uiState.selectedPackages.isNotEmpty() && uiState.multiQueue.isEmpty()) {
                                            viewModel.syncSelectedAppsIntoQueue()
                                        }
                                        navigateToRoute(ROUTE_CLEAN)
                                    },
                                    onGrantUsageAccessClick = { viewModel.openUsageAccessSettings() },
                                    onViewAllHistoryClick = { navigateToRoute(ROUTE_HISTORY) },
                                    onNavigateToAppsClick = { navigateToRoute(ROUTE_APPS) }
                                )
                            }

                            composable(ROUTE_APPS) {
                                AppsScreen(
                                    apps = filteredApps,
                                    totalDetectedCount = uiState.installedApps.size,
                                    selectedPackages = uiState.selectedPackages,
                                    searchQuery = uiState.searchQuery,
                                    filterMode = uiState.filterMode,
                                    sortMode = uiState.sortMode,
                                    isScanning = uiState.isScanning,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onFilterModeChange = viewModel::updateFilterMode,
                                    onSortModeChange = viewModel::updateSortMode,
                                    onToggleSelection = viewModel::toggleAppSelection,
                                    onSelectAll = viewModel::selectAllFilteredApps,
                                    onDeselectAll = viewModel::deselectAllApps,
                                    onRefreshScan = { viewModel.refreshAll() },
                                    onInspectApp = { app -> viewModel.setInspectAppPackage(app.packageName) },
                                    onSendSelectionToCache = {
                                        viewModel.startCacheSessionFromSelection()
                                        navigateToRoute(ROUTE_CLEAN)
                                    },
                                    onSendSelectionToQueue = {
                                        viewModel.syncSelectedAppsIntoQueue()
                                        navigateToRoute(ROUTE_CLEAN)
                                    }
                                )
                            }

                            composable(ROUTE_CLEAN) {
                                CleanHubScreen(
                                    uiState = uiState,
                                    onSelectTab = viewModel::setActiveCleanTab,
                                    onToggleAppSelection = viewModel::toggleAppSelection,
                                    onStartCacheSessionFromSelection = viewModel::startCacheSessionFromSelection,
                                    onExecuteCacheStep = viewModel::executeCacheStepForApp,
                                    onSkipCacheSessionStep = viewModel::skipCurrentCacheSessionApp,
                                    onToggleClearDataSelection = viewModel::toggleClearDataSelection,
                                    onRequestClearDataForApps = viewModel::requestClearDataForApps,
                                    onSyncSelectionIntoQueue = { viewModel.syncSelectedAppsIntoQueue() },
                                    onUpdateQueueAction = viewModel::updateQueueItemAction,
                                    onRemoveQueueItem = viewModel::removeQueueItem,
                                    onStartMultiQueue = viewModel::startMultiCleanQueue,
                                    onContinueQueueStep = viewModel::continueQueueAfterManualStep,
                                    onResetQueue = viewModel::resetQueueStatuses
                                )
                            }

                            composable(ROUTE_HISTORY) {
                                HistoryScreen(
                                    entries = historyEntries,
                                    onClearHistory = viewModel::clearHistory
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                SettingsScreen(
                                    isDarkTheme = isDarkTheme,
                                    confirmClearData = confirmClearData,
                                    compatibilityReport = uiState.compatibilityReport,
                                    onToggleDarkTheme = viewModel::setDarkTheme,
                                    onToggleConfirmClearData = viewModel::setConfirmClearData,
                                    onOpenUsageAccessSettings = viewModel::openUsageAccessSettings,
                                    onOpenSelfAppSettings = viewModel::openSelfAppSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
