package com.mccrosshair.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class Palette(
    val bg: Color,
    val card: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val chip: Color,
    val pill: Color,
    val pillFill: Color,
    val tile: Color,
    val warn: Color,
)

val DarkPalette = Palette(
    bg = Color(0xFF111318),
    card = Color(0xFF1D1F26),
    text = Color(0xFFE6E8EF),
    textDim = Color(0xFF8A8D96),
    accent = Color(0xFFB4C3FF),
    chip = Color(0xFF3F4453),
    pill = Color(0xFF2A2D36),
    pillFill = Color(0xFF334173),
    tile = Color(0xFF353A48),
    warn = Color(0xFFFFB4A9),
)

val LightPalette = Palette(
    bg = Color(0xFFF2F3F7),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF1A1C22),
    textDim = Color(0xFF6B6E78),
    accent = Color(0xFF4A5BC4),
    chip = Color(0xFFDDE1F0),
    pill = Color(0xFFE6E8F0),
    pillFill = Color(0xFFB9C4F2),
    tile = Color(0xFF353A48),
    warn = Color(0xFFB3261E),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.bg,
            background = p.bg, onBackground = p.text,
            surface = p.card, onSurface = p.text,
            surfaceContainerHigh = p.card,
            secondaryContainer = p.chip,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = Color.White,
            background = p.bg, onBackground = p.text,
            surface = p.card, onSurface = p.text,
            surfaceContainerHigh = p.card,
            secondaryContainer = p.chip,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
