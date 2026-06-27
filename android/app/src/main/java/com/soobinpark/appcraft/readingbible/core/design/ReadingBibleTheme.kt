package com.soobinpark.appcraft.readingbible.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette

private val PaperColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1B6B62),
    onPrimary = Color.White,
    secondary = Color(0xFF6D5E32),
    tertiary = Color(0xFF6A5778),
    background = Color(0xFFFBFAF6),
    surface = Color(0xFFFBFAF6),
    surfaceVariant = Color(0xFFE7E3D8),
    onSurface = Color(0xFF1D1C18),
)

private val EveningColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFE0B56B),
    onPrimary = Color(0xFF3E2E00),
    secondary = Color(0xFFCDBFA8),
    tertiary = Color(0xFFB7D4CC),
    background = Color(0xFF171512),
    surface = Color(0xFF201E1A),
    surfaceVariant = Color(0xFF3A352D),
    onSurface = Color(0xFFF1E8D8),
)

private val OledColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF77D7C8),
    onPrimary = Color(0xFF003731),
    secondary = Color(0xFFB8CCC8),
    tertiary = Color(0xFFFFE45E),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1D1D1D),
    onSurface = Color(0xFFECECEC),
)

private val HighContrastColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF005BD3),
    onPrimary = Color.White,
    secondary = Color(0xFF111111),
    tertiary = Color(0xFF004A9F),
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFE6E6E6),
    onSurface = Color.Black,
)

private val WarmLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF9A5B00),
    onPrimary = Color.White,
    secondary = Color(0xFF6D4F2C),
    tertiary = Color(0xFF1B6B62),
    background = Color(0xFFFFF6E8),
    surface = Color(0xFFFFF1D6),
    surfaceVariant = Color(0xFFFFE0B3),
    onSurface = Color(0xFF2D2215),
)

@Composable
fun ReadingBibleTheme(
    palette: ReadingPalette = ReadingPalette.Paper,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (palette) {
        ReadingPalette.Paper -> PaperColorScheme
        ReadingPalette.Evening -> EveningColorScheme
        ReadingPalette.Oled -> OledColorScheme
        ReadingPalette.HighContrast -> HighContrastColorScheme
        ReadingPalette.WarmLight -> WarmLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
