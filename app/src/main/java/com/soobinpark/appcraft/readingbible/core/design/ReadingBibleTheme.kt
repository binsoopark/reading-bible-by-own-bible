package com.soobinpark.appcraft.readingbible.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1B6B62),
    onPrimary = Color.White,
    secondary = Color(0xFF6D5E32),
    tertiary = Color(0xFF6A5778),
    background = Color(0xFFFBFAF6),
    surface = Color(0xFFFBFAF6),
    surfaceVariant = Color(0xFFE7E3D8),
    onSurface = Color(0xFF1D1C18),
)

@Composable
fun ReadingBibleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
