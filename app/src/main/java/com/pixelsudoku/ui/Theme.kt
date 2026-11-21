package com.pixelsudoku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class PixelColors(
    val bg: Color, val panel: Color, val ink: Color, val inkSoft: Color,
    val accent: Color, val accent2: Color, val cell: Color, val cellFixed: Color,
    val select: Color, val region: Color, val same: Color,
    val error: Color, val good: Color, val border: Color
)

val LightPixel = PixelColors(
    Color(0xFFF4F1DE), Color(0xFFFFFFFF), Color(0xFF2B2D42), Color(0xFF6B6F8A),
    Color(0xFFE07A5F), Color(0xFF3D85C6), Color(0xFFFFFFFF), Color(0xFFE9E4CC),
    Color(0xFFFFD166), Color(0xFFEDE8D0), Color(0xFFB7DDF5),
    Color(0xFFE63946), Color(0xFF3FA66B), Color(0xFF2B2D42)
)
val DarkPixel = PixelColors(
    Color(0xFF1B1D2E), Color(0xFF26293F), Color(0xFFF4F1DE), Color(0xFF9A9EB8),
    Color(0xFFE07A5F), Color(0xFF7FB7EA), Color(0xFF2B2E47), Color(0xFF383C5C),
    Color(0xFF9C7A1E), Color(0xFF30344F), Color(0xFF2F5575),
    Color(0xFFFF6B77), Color(0xFF4FC08A), Color(0xFFF4F1DE)
)

val LocalPixel = staticCompositionLocalOf { LightPixel }

@Composable
fun PixelTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    CompositionLocalProvider(LocalPixel provides (if (dark) DarkPixel else LightPixel)) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
    }
}
