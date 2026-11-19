package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.system.SystemCompatibilityInspector
import com.example.model.CleanQueueItem
import com.example.model.CleanTab
import com.example.model.InstalledAppInfo
import com.example.model.PendingSystemVerification
import com.example.model.QueueAction
import com.example.model.QueueItemStatus
import com.example.ui.CleanMasterUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalCleanAccentColors
import com.example.util.ByteFormatter

@Composable
fun CleanHubScreen(
    uiState: CleanMasterUiState,
    onSelectTab: (CleanTab) -> Unit,
    onToggleAppSelection: (String) -> Unit,
    onStartCacheSessionFromSelection: () -> Unit,
    onExecuteCacheStep: (InstalledAppInfo) -> Unit,
    onSkipCacheSessionStep: () -> Unit,
    onToggleClearDataSelection: (String) -> Unit,
    onRequestClearDataForApps: (List<InstalledAppInfo>) -> Unit,
    onSyncSelectionIntoQueue: () -> Unit,
    onUpdateQueueAction: (String, QueueAction) -> Unit,
    onRemoveQueueItem: (String) -> Unit,
    onStartMultiQueue: () -> Unit,
    onContinueQueueStep: (Boolean) -> Unit,
    onResetQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Centro de Limpieza Oficial",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CleanTab.entries.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = uiState.activeCleanTab == tab,
                        onClick = { onSelectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = CleanTab.entries.size),
                        modifier = Modifier.testTag("clean_tab_${tab.name.lowercase()}")
                    ) {
                        Text(
                            text = tab.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        when (uiState.activeCleanTab) {
            CleanTab.CACHE -> CacheManagerSection(
                uiState = uiState,
                onToggleAppSelection = onToggleAppSelection,
                onStartCacheSession = onStartCacheSessionFromSelection,
                onExecuteCacheStep = onExecuteCacheStep,
                onSkipSessionStep = onSkipCacheSessionStep
            )
            CleanTab.CLEAR_DATA -> ClearDataSection(
                uiState = uiState,
                onToggleClearDataSelection = onToggleClearDataSelection,
                onRequestClearDataForApps = onRequestClearDataForApps
            )
            CleanTab.MULTI_QUEUE -> MultiCleanQueueSection(
                uiState = uiState,
                onSyncSelectionIntoQueue = onSyncSelectionIntoQueue,
                onUpdateQueueAction = onUpdateQueueAction,
                onRemoveQueueItem = onRemoveQueueItem,
                onStartMultiQueue = onStartMultiQueue,
                onContinueQueueStep = onContinueQueueStep,
                onResetQueue = onResetQueue,
                onReopenAppSettings = { pkg ->
                    uiState.installedApps.find { it.packageName == pkg }?.let(onExecuteCacheStep)
                }
            )
        }
    }
}

@Composable
private fun CacheManagerSection(
    uiState: CleanMasterUiState,
    onToggleAppSelection: (String) -> Unit,
    onStartCacheSession: () -> Unit,
    onExecuteCacheStep: (InstalledAppInfo) -> Unit,
    onSkipSessionStep: () -> Unit
) {
    val sessionPkgs = uiState.cacheSessionPackages
    val currentPkg = sessionPkgs.getOrNull(uiState.cacheSessionIndex)
    val currentSessionApp = currentPkg?.let { pkg -> uiState.installedApps.find { it.packageName == pkg } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Administrador de caché (API oficial de Android)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clean Master AI limpia directamente su propia caché interna. Para aplicaciones de terceros, Android exige confirmación manual: abriremos la ficha oficial de la app y, al regresar, verificaremos si el caché disminuyó.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SystemCompatibilityInspector.getOemStoragePathHint(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        if (currentSessionApp != null) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Asistente paso a paso (${uiState.cacheSessionIndex + 1} de ${sessionPkgs.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIconThumbnail(currentSessionApp.icon, currentSessionApp.appName)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSessionApp.appName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Caché actual: ${ByteFormatter.formatBytes(currentSessionApp.cacheBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onExecuteCacheStep(currentSessionApp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("cache_session_open_button")
                            ) {
                                Icon(
                                    imageVector = if (currentSessionApp.isSelfApp) Icons.Rounded.CleaningServices else Icons.Rounded.OpenInNew,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (currentSessionApp.isSelfApp) "Limpiar ahora" else "Abrir Ajustes")
                            }
                            OutlinedButton(
                                onClick = onSkipSessionStep,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("cache_session_next_button")
                            ) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Siguiente")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seleccionadas: ${uiState.selectedPackages.size}",
                    style = MaterialTheme.typography.titleSmall
                )
                Button(
                    onClick = onStartCacheSession,
                    enabled = uiState.selectedPackages.isNotEmpty(),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("cache_start_guided_button")
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar limpieza de caché")
                }
            }
        }

        items(uiState.installedApps, key = { "cache_${it.packageName}" }) { app ->
            val selected = app.packageName in uiState.selectedPackages
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleAppSelection(app.packageName) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleAppSelection(app.packageName) }
                    )
                    AppIconThumbnail(app.icon, app.appName, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Caché: ${ByteFormatter.formatBytes(app.cacheBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(
                        onClick = { onExecuteCacheStep(app) },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (app.isSelfApp) "Limpiar" else "Ajustes")
                    }
                }
            }
        }
    }
}

@Composable
private fun ClearDataSection(
    uiState: CleanMasterUiState,
    onToggleClearDataSelection: (String) -> Unit,
    onRequestClearDataForApps: (List<InstalledAppInfo>) -> Unit
) {
    val accents = LocalCleanAccentColors.current
    val selectedForData = uiState.installedApps.filter { it.packageName in uiState.clearDataPackages }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = accents.warning.copy(alpha = 0.13f)
                ),
                border = BorderStroke(1.dp, accents.warning.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Advertencia",
                        tint = accents.warning
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.section_clear_data),
                            style = MaterialTheme.typography.titleSmall,
                            color = accents.warning
                        )
                        Text(
                            text = stringResource(R.string.warning_clear_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Se solicitará confirmación explícita antes de abrir la pantalla oficial de Android de cada aplicación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (selectedForData.isNotEmpty()) {
            item {
                Button(
                    onClick = { onRequestClearDataForApps(selectedForData) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("clear_data_selected_button")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Revisar y borrar datos (${selectedForData.size} sel.)")
                }
            }
        }

        items(uiState.installedApps, key = { "data_${it.packageName}" }) { app ->
            val selected = app.packageName in uiState.clearDataPackages
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleClearDataSelection(app.packageName) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleClearDataSelection(app.packageName) }
                    )
                    AppIconThumbnail(app.icon, app.appName, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Datos: ${ByteFormatter.formatBytes(app.dataBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { onRequestClearDataForApps(listOf(app)) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clear_data_row_button_${app.packageName}")
                    ) {
                        Text("Borrar datos")
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiCleanQueueSection(
    uiState: CleanMasterUiState,
    onSyncSelectionIntoQueue: () -> Unit,
    onUpdateQueueAction: (String, QueueAction) -> Unit,
    onRemoveQueueItem: (String) -> Unit,
    onStartMultiQueue: () -> Unit,
    onContinueQueueStep: (Boolean) -> Unit,
    onResetQueue: () -> Unit,
    onReopenAppSettings: (String) -> Unit
) {
    val queue = uiState.multiQueue
    val pendingConfirm: PendingSystemVerification? = uiState.pendingVerification
        ?.takeIf { it.mode == com.example.model.VerificationMode.MULTI_QUEUE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Cola de operaciones múltiples",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Asigna una acción por aplicación (Limpiar caché, Borrar datos o No modificar). Clean Master AI solo marcará como «Completada» las operaciones verificadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSyncSelectionIntoQueue,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("queue_import_selected_button")
                        ) {
                            Text("Importar seleccionadas (${uiState.selectedPackages.size})")
                        }
                        if (queue.isNotEmpty()) {
                            IconButton(
                                onClick = onResetQueue,
                                modifier = Modifier.testTag("queue_reset_button")
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = "Reiniciar estados de la cola")
                            }
                        }
                    }
                }
            }
        }

        if (pendingConfirm != null) {
            item {
                val accents = LocalCleanAccentColors.current
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = accents.warning.copy(alpha = 0.14f)),
                    border = BorderStroke(1.5.dp, accents.warning)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Confirmación requerida: ${pendingConfirm.appName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = accents.warning
                        )
                        Text(
                            text = "Si ya completaste «${pendingConfirm.action.label}» en Ajustes del sistema y Android no reportó reducción de tamaño, puedes reabrir Ajustes o continuar sin marcarla como completada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onReopenAppSettings(pendingConfirm.packageName) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Reabrir Ajustes")
                            }
                            Button(
                                onClick = { onContinueQueueStep(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("queue_skip_unverified_button")
                            ) {
                                Text("Continuar cola")
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onStartMultiQueue,
                enabled = queue.isNotEmpty() && !uiState.isQueueRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("queue_start_clean_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_start_clean),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (queue.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "No hay operaciones en la cola",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Selecciona aplicaciones en la pestaña Apps y pulsa «Importar seleccionadas».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(queue, key = { it.packageName }) { item ->
                QueueItemCard(
                    item = item,
                    onUpdateAction = { action -> onUpdateQueueAction(item.packageName, action) },
                    onRemove = { onRemoveQueueItem(item.packageName) }
                )
            }
        }
    }
}

@Composable
private fun QueueItemCard(
    item: CleanQueueItem,
    onUpdateAction: (QueueAction) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("queue_item_${item.packageName}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusBadgePill(status = item.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QueueAction.entries.forEach { action ->
                    FilterChip(
                        selected = item.action == action,
                        onClick = { onUpdateAction(action) },
                        label = {
                            Text(
                                text = when (action) {
                                    QueueAction.CLEAR_CACHE -> "Caché"
                                    QueueAction.CLEAR_DATA -> "Datos"
                                    QueueAction.SKIP -> "No modificar"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            if (item.recoveredBytes != null) {
                Text(
                    text = "Espacio recuperado verificado: ${ByteFormatter.formatBytes(item.recoveredBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (item.statusNote.isNotBlank()) {
                Text(
                    text = item.statusNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRemove) {
                    Text("Quitar de la cola")
                }
            }
        }
    }
}
