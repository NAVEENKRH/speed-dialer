package com.naveen.callqueue.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandBlue = Color(0xFF1565C0)
val BrandBlueDark = Color(0xFF0D47A1)
val BrandBlueLight = Color(0xFFE3F0FD)
val SuccessGreen = Color(0xFF2E7D32)
val SuccessGreenLight = Color(0xFFE3F3E5)
val WarnAmber = Color(0xFFB8860B)
val WarnAmberLight = Color(0xFFFCF0DA)
val DangerRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = SuccessGreen,
    secondaryContainer = SuccessGreenLight,
    onSecondaryContainer = SuccessGreen,
    tertiary = WarnAmber,
    tertiaryContainer = WarnAmberLight,
    onTertiaryContainer = WarnAmber,
    error = DangerRed,
    surface = Color(0xFFFAFBFE),
    background = Color(0xFFFAFBFE)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DB2ED),
    onPrimary = Color(0xFF08305C),
    primaryContainer = Color(0xFF0D3E75),
    onPrimaryContainer = Color(0xFFD3E6FB),
    secondary = Color(0xFF8FCE93),
    secondaryContainer = Color(0xFF1F4823),
    onSecondaryContainer = Color(0xFFC8ECC9),
    tertiary = Color(0xFFE3C177),
    tertiaryContainer = Color(0xFF4A3A10),
    onTertiaryContainer = Color(0xFFF3DFAF),
    error = Color(0xFFE58F8F)
)

@Composable
fun CallQueueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Deliberately not using dynamic/Material-You colors: a consistent brand look
    // (matching the app icon) regardless of the phone's wallpaper.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
