package com.jarvis.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object JarvisColors {
    val DeepBlack = Color(0xFF03060C)
    val DarkNavy = Color(0xFF081020)
    val ElevatedNavy = Color(0xFF0D182E)
    val HoloCyan = Color(0xFF38BDF8)
    val CoreBlue = Color(0xFF0EA5E9)
    val ArcGlow = Color(0xFF7DD3FC)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val BorderHairline = Color(0x2E38BDF8)
    val AlertCoral = Color(0xFFF87171)
}

private val JarvisDarkScheme = darkColorScheme(
    primary = JarvisColors.HoloCyan,
    onPrimary = JarvisColors.DeepBlack,
    secondary = JarvisColors.CoreBlue,
    onSecondary = JarvisColors.DeepBlack,
    background = JarvisColors.DeepBlack,
    onBackground = JarvisColors.TextPrimary,
    surface = JarvisColors.DarkNavy,
    onSurface = JarvisColors.TextPrimary,
    surfaceVariant = JarvisColors.ElevatedNavy,
    onSurfaceVariant = JarvisColors.TextSecondary,
    outline = JarvisColors.BorderHairline,
    error = JarvisColors.AlertCoral
)

private val JarvisTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisDarkScheme,
        typography = JarvisTypography,
        content = content
    )
}
