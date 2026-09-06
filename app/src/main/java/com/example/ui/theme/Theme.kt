package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// ==========================================
// Whacka Light & Dark Color Schemes
// ==========================================

private val WhackaLightColorScheme = lightColorScheme(
    primary = WhackaPrimaryAccent,             // Primary Accent #0F6E60
    onPrimary = Color.White,
    primaryContainer = WhackaLightPrimaryContainer,
    onPrimaryContainer = WhackaPrimaryAccent,
    inversePrimary = WhackaLightPrimaryDark,
    secondary = WhackaSecondaryAccent,         // Secondary Accent #C78222
    onSecondary = Color.White,
    secondaryContainer = WhackaAmberBg,
    onSecondaryContainer = WhackaAmberDark,
    tertiary = WhackaSecondaryAccent,          // Secondary Accent #C78222
    onTertiary = Color.White,
    tertiaryContainer = WhackaAmberBg,
    onTertiaryContainer = WhackaAmberDark,
    background = WhackaBackground,             // Background #FAF8F2
    onBackground = WhackaTextPrimary,          // Text Primary 75% Black (0xBF000000)
    surface = WhackaSurface,                   // Surface/Cards #FFFFFF
    onSurface = WhackaTextPrimary,             // Text Primary 75% Black (0xBF000000)
    surfaceVariant = WhackaLightSurfaceContainer,
    onSurfaceVariant = WhackaTextSecondary,    // Text Secondary 45% Black (0x73000000)
    surfaceContainerLowest = WhackaSurface,
    surfaceContainerLow = WhackaLightSurfaceContainer,
    surfaceContainer = WhackaSurface,
    surfaceContainerHigh = WhackaLightSurfaceContainer,
    surfaceContainerHighest = Color(0xFFECEAE2),
    outline = WhackaBorder,                    // Borders 10% Black (0x1A000000)
    outlineVariant = WhackaLightBorderSubtle,
    error = WhackaRed,                         // Soft Red #EF4444
    onError = Color.White,
    errorContainer = WhackaRedBg,
    onErrorContainer = Color(0xFF991B1B)
)

private val WhackaDarkColorScheme = darkColorScheme(
    primary = WhackaDarkPrimary,               // Bright Teal
    onPrimary = Color(0xFF0F172A),
    primaryContainer = WhackaDarkPrimaryContainer,
    onPrimaryContainer = WhackaDarkOnPrimaryContainer,
    inversePrimary = WhackaDarkPrimaryFixedDim,
    secondary = WhackaAmberLight,
    onSecondary = Color.White,
    secondaryContainer = WhackaAmberDarkBg,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = WhackaAmberLight,
    onTertiary = Color.White,
    tertiaryContainer = WhackaAmberDarkBg,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = WhackaDarkBackground,
    onBackground = WhackaDarkText,
    surface = WhackaDarkSurface,
    onSurface = WhackaDarkText,
    surfaceVariant = WhackaDarkSurfaceContainer,
    onSurfaceVariant = WhackaDarkTextMuted,
    surfaceContainerLowest = WhackaDarkBackground,
    surfaceContainerLow = WhackaDarkSurface,
    surfaceContainer = WhackaDarkSurface,
    surfaceContainerHigh = WhackaDarkSurfaceContainer,
    surfaceContainerHighest = WhackaDarkSurfaceHighest,
    outline = WhackaDarkBorder,
    outlineVariant = Color(0xFF334155),
    error = WhackaRed,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

// Whacka Shapes Geometry: Modals & Cards 24dp, Inputs 16dp, Dialogs/Sheets 28dp
val WhackaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),      // Input Fields (16.dp)
    medium = RoundedCornerShape(24.dp),     // Cards (24.dp)
    large = RoundedCornerShape(24.dp),      // Modals & Dialogs (24.dp)
    extraLarge = RoundedCornerShape(28.dp)  // Large Dialogs & Surfaces (28.dp)
)

@Composable
fun WhackaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WhackaDarkColorScheme else WhackaLightColorScheme

    // Force RTL (Right-to-Left) layout globally for Whacka Arabic UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WhackaShapes,
            content = content
        )
    }
}

// Backward-compatible alias
@Composable
fun FocusCraftTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    WhackaTheme(darkTheme = darkTheme, content = content)
}
