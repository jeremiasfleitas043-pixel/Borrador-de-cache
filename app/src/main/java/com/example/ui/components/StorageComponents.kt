package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceStorageInfo
import com.example.model.InstalledAppInfo
import com.example.model.QueueItemStatus
import com.example.ui.theme.LocalCleanAccentColors
import com.example.util.ByteFormatter

@Composable
fun StorageHeroCard(
    storage: DeviceStorageInfo?,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val targetFraction = storage?.usedFraction ?: 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "storage_gauge_progress"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    primaryColor.copy(alpha = 0.45f),
                    secondaryColor.copy(alpha = 0.45f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.12f),
                            primaryColor.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(120f, 120f),
                        radius = 650f
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier.size(124.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 13.dp.toPx()
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        drawArc(
                            color = trackColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        if (animatedFraction > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(primaryColor, secondaryColor, primaryColor)
                                ),
                                startAngle = 135f,
                                sweepAngle = 270f * animatedFraction,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val pct = if (storage != null) (storage.usedFraction * 100).toInt() else 0
                        Text(
                            text = if (isScanning && storage == null) "…" else "$pct%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ocupado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StorageMetricRow(
                        label = "Almacenamiento total",
                        value = ByteFormatter.formatBytes(storage?.totalBytes),
                        dotColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StorageMetricRow(
                        label = "Ocupado",
                        value = ByteFormatter.formatBytes(storage?.usedBytes),
                        dotColor = secondaryColor
                    )
                    StorageMetricRow(
                        label = "Disponible",
                        value = ByteFormatter.formatBytes(storage?.freeBytes),
                        dotColor = primaryColor
                    )
                }
            }

            if (storage != null) {
                Text(
                    text = "Fuente del sistema: ${storage.sourceLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StorageMetricRow(
    label: String,
    value: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppIconThumbnail(
    drawable: Drawable?,
    appName: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) { drawable?.toSafeBitmap() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Icono de $appName",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = "Icono de $appName",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun Drawable.toSafeBitmap(): Bitmap? {
    return try {
        if (this is BitmapDrawable && bitmap != null) {
            bitmap
        } else {
            val width = intrinsicWidth.coerceIn(48, 160)
            val height = intrinsicHeight.coerceIn(48, 160)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bmp
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun StatusBadgePill(
    status: QueueItemStatus,
    modifier: Modifier = Modifier
) {
    val accents = LocalCleanAccentColors.current
    val (bg, fg) = when (status) {
        QueueItemStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        QueueItemStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        QueueItemStatus.REQUIRES_CONFIRMATION -> accents.warning.copy(alpha = 0.16f) to accents.warning
        QueueItemStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        QueueItemStatus.NOT_ALLOWED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.error
        QueueItemStatus.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier.border(1.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(999.dp)),
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ClearDataConfirmDialog(
    apps: List<InstalledAppInfo>,
    requireExtraAck: Boolean,
    onConfirmFirstApp: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val targetApp = apps.firstOrNull() ?: return
    val accents = LocalCleanAccentColors.current
    var acknowledged by remember(targetApp.packageName, requireExtraAck) {
        mutableStateOf(!requireExtraAck)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Advertencia de borrado de datos",
                tint = accents.warning,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = "Confirmar borrado de datos",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Aplicación seleccionada: ${targetApp.appName} (${targetApp.packageName})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accents.warning.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accents.warning.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = stringResource(R.string.warning_clear_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Text(
                    text = "Clean Master AI nunca borra datos en segundo plano. Al confirmar, se abrirá la pantalla oficial de Android para que autorices manualmente el borrado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (requireExtraAck) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = acknowledged,
                            onCheckedChange = { acknowledged = it },
                            modifier = Modifier.testTag("clear_data_extra_ack_checkbox")
                        )
                        Text(
                            text = "Entiendo que se pueden perder cuentas y datos locales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmFirstApp(targetApp) },
                enabled = acknowledged,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_clear_data_button")
            ) {
                Text("Abrir Ajustes y continuar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_clear_data_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}
