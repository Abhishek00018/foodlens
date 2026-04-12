package com.caltrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Premium Dark palette
val DarkBg = Color(0xFF0D0D14)
val DarkSurface = Color(0xFF16161F)
val DarkSurfaceVariant = Color(0xFF1E1E2A)
val NeonLime = Color(0xFF80FF00)
val NeonLimeDim = Color(0xFF5CB800)
val AccentOrange = Color(0xFFFF6F00)
val TextWhite = Color(0xFFF2F2F2)
val TextSecondary = Color(0xFF8E8E9A)
val CardBorder = Color(0xFF2A2A38)

// Light palette (fallback for non-dark mode)
val LightBg = Color(0xFF0D0D14)
val LightSurface = Color(0xFF16161F)

private val CaltrackDarkScheme = darkColorScheme(
    primary = NeonLime,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A3D00),
    onPrimaryContainer = NeonLime,
    secondary = AccentOrange,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D2200),
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = Color(0xFF222230),
)

// Force dark mode always (Premium Dark style)
private val CaltrackLightScheme = CaltrackDarkScheme

val CaltrackTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 48.sp, color = TextWhite),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, color = TextWhite),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TextWhite),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextWhite),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextWhite),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextWhite),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TextWhite),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = TextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NeonLime),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = TextSecondary),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = TextSecondary),
)

@Composable
fun CaltrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CaltrackDarkScheme,
        typography = CaltrackTypography,
        content = content
    )
}
