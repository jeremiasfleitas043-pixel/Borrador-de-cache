package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CompatibilityReport

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    confirmClearData: Boolean,
    compatibilityReport: CompatibilityReport?,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleConfirmClearData: (Boolean) -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenSelfAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            SettingsSectionCard(
                title = "Apariencia e idioma",
                icon = Icons.Rounded.DarkMode
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tema oscuro", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (isDarkTheme) "Activo (oscuro con acentos verde y azul)" else "Modo claro activo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("settings_dark_theme_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text("Idioma de la aplicación", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Español (ES) — Predeterminado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Seguridad de borrado",
                icon = Icons.Rounded.VerifiedUser
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Confirmación de borrado de datos", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Muestra advertencia obligatoria y confirmación previa antes de abrir Ajustes de Android en la sección BORRAR DATOS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = confirmClearData,
                        onCheckedChange = onToggleConfirmClearData,
                        modifier = Modifier.testTag("settings_confirm_clear_data_switch")
                    )
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Información de permisos",
                icon = Icons.Rounded.Security
            ) {
                val hasUsage = compatibilityReport?.hasUsageAccessPermission == true
                Text(
                    text = "1. Visibilidad de aplicaciones (<queries>):",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Clean Master AI declara únicamente consultas dirigidas para aplicaciones lanzables (ACTION_MAIN / CATEGORY_LAUNCHER) y pantallas de detalles. No utiliza QUERY_ALL_PACKAGES, ya que Google Play restringe ese permiso a apps cuya función central exija inventario completo de paquetes de sistema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "2. Acceso a estadísticas de uso (PACKAGE_USAGE_STATS):",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Estado actual: ${if (hasUsage) "Concedido" else "No concedido"}. Es un permiso especial opcional para leer Aplicación, Datos y Caché mediante StorageStatsManager.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasUsage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenUsageAccessSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_usage_access_button")
                    ) {
                        Text("Acceso de uso")
                    }
                    OutlinedButton(
                        onClick = onOpenSelfAppSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Info de la app")
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Información de compatibilidad",
                icon = Icons.Rounded.PhonelinkSetup
            ) {
                if (compatibilityReport != null) {
                    Text(
                        text = "${compatibilityReport.manufacturer} ${compatibilityReport.model} · ${compatibilityReport.androidVersionLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = compatibilityReport.apiLimitationsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ruta detectada para este fabricante: ${compatibilityReport.oemSettingsPathHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Compatibilidad multi-fabricante: adaptada para Samsung (One UI), Xiaomi/Redmi/POCO (HyperOS/MIUI), Motorola, ZTE/Nubia, Tecno/Infinix y Android estándar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Acerca de Clean Master AI",
                icon = Icons.Rounded.Info
            ) {
                Text(
                    text = "Clean Master AI v1.0 · Paquete: com.cleanmaster.ai",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Arquitectura 100% nativa en Kotlin y Jetpack Compose. Sin servidores externos, sin acceso root, sin comandos ADB y sin servicios de accesibilidad automatizados. Todas las operaciones respetan las políticas y API oficiales de Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
