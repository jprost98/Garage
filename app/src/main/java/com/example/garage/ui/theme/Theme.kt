package com.example.garage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    error = DangerRed,
    errorContainer = DangerContainer,
    onErrorContainer = DangerOnContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = TealContainer
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnContainer,
    primaryContainer = TealOnContainer,
    onPrimaryContainer = TealContainer,
    error = DangerRed,
    background = BackgroundDark,
    surface = SurfaceDark
)

@Composable
fun GarageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GarageTypography,
        content = content
    )
}
