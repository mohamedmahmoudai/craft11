package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Whacka Design System - Color Palette
// ==========================================

// Exact Whacka Blueprint Colors
val WhackaBackground = Color(0xFFFAF8F2)             // Background #FAF8F2
val WhackaSurface = Color(0xFFFFFFFF)                // Surface/Cards #FFFFFF
val WhackaPrimaryAccent = Color(0xFF0F6E60)          // Primary Accent #0F6E60
val WhackaSecondaryAccent = Color(0xFFC78222)        // Secondary Accent #C78222
val WhackaTeal = WhackaPrimaryAccent
val WhackaForest = WhackaPrimaryAccent
val WhackaTextPrimary = Color(0xBF000000)            // Text Primary 75% Black (0xBF000000)
val WhackaTextSecondary = Color(0x73000000)          // Text Secondary 45% Black (0x73000000)
val WhackaBorder = Color(0x1A000000)                 // Borders 10% Black (0x1A000000)
val WhackaPrimaryShadow = Color(0xFF0F6E60).copy(alpha = 0.18f) // Custom elevation/shadow (alpha 0.18)

// Light Mode (Primary Default)
val WhackaLightBackground = WhackaBackground
val WhackaLightSurface = WhackaSurface
val WhackaLightSurfaceContainer = Color(0xFFF5F3EB)
val WhackaLightPrimary = WhackaPrimaryAccent
val WhackaLightPrimaryDark = Color(0xFF0A5147)
val WhackaLightPrimaryContainer = Color(0xFFD4EDE8)
val WhackaLightOnPrimaryContainer = WhackaPrimaryAccent
val WhackaLightText = WhackaTextPrimary
val WhackaLightTextMuted = WhackaTextSecondary
val WhackaLightBorder = WhackaBorder
val WhackaLightBorderSubtle = Color(0x0D000000)

// Dark Mode (Whacka Charcoal Mode)
val WhackaDarkBackground = Color(0xFF141716)
val WhackaDarkSurface = Color(0xFF1F2423)
val WhackaDarkSurfaceContainer = Color(0xFF282F2E)
val WhackaDarkSurfaceHighest = Color(0xFF353D3B)
val WhackaDarkPrimary = Color(0xFF2DD4BF)
val WhackaDarkPrimaryFixedDim = Color(0xFF14B8A6)
val WhackaDarkPrimaryContainer = Color(0xFF0F6E60)
val WhackaDarkOnPrimaryContainer = Color(0xFFD4EDE8)
val WhackaDarkText = Color(0xFFFAF8F2)
val WhackaDarkTextMuted = Color(0xFFB5BEBC)
val WhackaDarkBorder = Color(0x33FFFFFF)
val WhackaDarkBorderSubtle = Color(0x1AFFFFFF)

// Functional Shared Accents
val WhackaAmber = WhackaSecondaryAccent              // Secondary Accent #C78222
val WhackaAmberLight = Color(0xFFE59835)
val WhackaAmberBg = Color(0xFFFEF3C7)                // Soft Amber Container
val WhackaAmberDark = Color(0xFF9E6415)
val WhackaAmberDarkBg = Color(0xFF451A03)

val WhackaEmerald = Color(0xFF10B981)                // Soft Green
val WhackaEmeraldBg = Color(0xFFD1FAE5)

val WhackaRed = Color(0xFFEF4444)                    // Soft Red
val WhackaRedBg = Color(0xFFFEE2E2)

// Compatibility Aliases
val DeepBackground = WhackaDarkBackground
val DarkNavyBackground = WhackaDarkBackground
val SurfaceDark = WhackaDarkSurface
val SurfaceContainer = WhackaDarkSurface
val SurfaceContainerHigh = WhackaDarkSurfaceContainer
val SurfaceContainerHighest = WhackaDarkSurfaceHighest
val SurfaceSlateCard = WhackaDarkSurface
val SurfaceCardBorder = WhackaDarkBorderSubtle

val TextOnSurface = WhackaDarkText
val TextOnSurfaceVariant = WhackaDarkTextMuted
val TextSlateMuted = WhackaDarkTextMuted
val TextOutline = Color(0xFF475569)
val TextOutlineVariant = Color(0xFF334155)

val IndigoPrimary = WhackaPrimaryAccent
val IndigoPrimaryFixedDim = WhackaDarkPrimaryFixedDim
val IndigoPrimaryContainer = WhackaLightPrimaryContainer
val IndigoInversePrimary = WhackaDarkPrimary

val SuccessSecondary = WhackaEmerald
val SecondaryContainer = Color(0xFF059669)
val OnSecondaryContainer = Color(0xFFECFDF5)

val AmberTertiary = WhackaSecondaryAccent
val AmberTertiaryContainer = WhackaAmberDark

val ErrorRed = WhackaRed
val ErrorContainer = WhackaRedBg

val LightBackground = WhackaLightBackground
val LightSurface = WhackaLightSurface
val LightSurfaceContainer = WhackaLightSurfaceContainer
val LightTextOnSurface = WhackaLightText
val LightTextOnSurfaceVariant = WhackaLightTextMuted
val LightOutline = WhackaLightBorder
