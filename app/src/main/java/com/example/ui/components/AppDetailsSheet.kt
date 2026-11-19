package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.system.SystemCompatibilityInspector
import com.example.model.InstalledAppInfo
import com.example.model.QueueAction
import com.example.util.ByteFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsSheet(
    app: InstalledAppInfo,
    hasUsageStatsPermission: Boolean,
    onGrantUsageAccess: () -> Unit,
    onCleanCache: (InstalledAppInfo) -> Unit,
    onRequestClearData: (InstalledAppInfo) -> Unit,
    onAddToQueue: (InstalledAppInfo, QueueAction) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIconThumbnail(drawable = app.icon, appName = app.appName, size = 52.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val typeText = when {
                        app.isSelfApp -> "Esta aplicación (limpieza directa disponible)"
                        app.isSystemApp -> "Aplicación del sistema"
                        else -> "Aplicación instalada"
                    }
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Text(
                text = "Desglose de almacenamiento (StorageStatsManager)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailSizeRow("Aplicación", ByteFormatter.formatBytes(app.appBytes))
                    DetailSizeRow("Datos", ByteFormatter.formatBytes(app.dataBytes))
                    DetailSizeRow("Caché", ByteFormatter.formatBytes(app.cacheBytes))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    DetailSizeRow("Almacenamiento total", ByteFormatter.formatBytes(app.totalBytes))
                    if (app.totalBytes == null && app.apkSizeBytes != null) {
                        DetailSizeRow("Tamaño APK base", ByteFormatter.formatBytes(app.apkSizeBytes))
                    }
                }
            }

            if (!hasUsageStatsPermission) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Android requiere el permiso especial de Acceso a estadísticas de uso para medir Aplicación, Datos y Caché.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedButton(
                            onClick = onGrantUsageAccess,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Conceder acceso de uso")
                        }
                    }
                }
            }

            Text(
                text = SystemCompatibilityInspector.getOemStoragePathHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onCleanCache(app) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_clean_cache_button")
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Caché")
                }
                Button(
                    onClick = { onRequestClearData(app) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_clear_data_button")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar datos")
                }
            }

            OutlinedButton(
                onClick = { onAddToQueue(app, QueueAction.CLEAR_CACHE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sheet_add_queue_button")
            ) {
                Icon(Icons.Rounded.AddToQueue, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar a cola de limpieza múltiple")
            }
        }
    }
}

@Composable
private fun DetailSizeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
