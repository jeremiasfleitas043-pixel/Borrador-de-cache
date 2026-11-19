package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class CleanAccentColors(
    val warning: Color,
    val success: Color,
    val info: Color
)

val LocalCleanAccentColors = staticCompositionLocalOf {
    CleanAccentColors(
        warning = WarningAmberDark,
        success = EmeraldPrimaryDark,
        info = ElectricBlueSecondaryDark
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = ElectricBlueSecondaryDark,
    onSecondary = OnElectricBlueDark,
    secondaryContainer = ElectricBlueContainerDark,
    onSecondaryContainer = OnElectricBlueContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = OnTealTertiaryDark,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = ErrorCoralDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = TechBlueSecondaryLight,
    onSecondary = OnTechBlueLight,
    secondaryContainer = TechBlueContainerLight,
    onSecondaryContainer = OnTechBlueContainerLight,
    tertiary = TealTertiaryLight,
    onTertiary = OnTealTertiaryLight,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = ErrorRedLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val CleanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CleanMasterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accentColors = if (darkTheme) {
        CleanAccentColors(
            warning = WarningAmberDark,
            success = EmeraldPrimaryDark,
            info = ElectricBlueSecondaryDark
        )
    } else {
        CleanAccentColors(
            warning = WarningAmberLight,
            success = EmeraldPrimaryLight,
            info = TechBlueSecondaryLight
        )
    }

    CompositionLocalProvider(LocalCleanAccentColors provides accentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = CleanShapes,
            content = content
        )
    }
}
