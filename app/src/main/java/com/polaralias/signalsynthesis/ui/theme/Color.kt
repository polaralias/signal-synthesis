package com.polaralias.signalsynthesis.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Branding Colors
val BrandPrimary = Color(0xFF4285F4)
val BrandSecondary = Color(0xFF9D50BB)

// Dark Theme Colors
val DarkDeepBackground = Color(0xFF0A0B10)
val DarkSurface = Color(0xFF161821)
val DarkGlassBackground = Color(0x33FFFFFF)
val DarkGlassBorder = Color(0x4DFFFFFF)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Light Theme Colors
val LightDeepBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightGlassBackground = Color(0x1A000000)
val LightGlassBorder = Color(0x33000000)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)

// Status Colors
val SuccessGreen = Color(0xFF34A853)
val WarningOrange = Color(0xFFFBBC05)
val ErrorRed = Color(0xFFEA4335)
val InfoBlue = Color(0xFF4285F4)

// Rainbow Spectrum Colors (Premium Palette)
val Rainbow1 = Color(0xFF4285F4) // Blue
val Rainbow2 = Color(0xFF34A853) // Green
val Rainbow3 = Color(0xFFFBBC05) // Yellow/Orange
val Rainbow4 = Color(0xFFEA4335) // Red
val Rainbow5 = Color(0xFF9D50BB) // Purple
val Rainbow6 = Color(0xFF00D2FF) // Cyan

// Glassmorphism specific - Dark
val GlassWhite = Color(0xFFFFFFFF).copy(alpha = 0.05f)
val GlassWhiteBorder = Color(0xFFFFFFFF).copy(alpha = 0.2f)

// Glassmorphism specific - Light
val GlassBlack = Color(0xFF000000).copy(alpha = 0.05f)
val GlassBlackBorder = Color(0xFF000000).copy(alpha = 0.1f)

// Aliases for compatibility during overhaul
val RainbowBlue = Rainbow1
val RainbowGreen = Rainbow2
val RainbowYellow = Rainbow3
val RainbowOrange = Rainbow3
val RainbowRed = Rainbow4
val RainbowPurple = Rainbow5
val RainbowPink = Rainbow4
val NeonBlue = Rainbow1
val NeonPurple = Rainbow5
val NeonGreen = Rainbow2
val NeonRed = Rainbow4
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary

