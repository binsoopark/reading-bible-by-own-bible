package com.soobinpark.appcraft.readingbible.domain.model

enum class ReadingPalette {
    Paper,
    Evening,
    Oled,
    HighContrast,
    WarmLight,
}

data class ReadingStyle(
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.55f,
    val palette: ReadingPalette = ReadingPalette.Paper,
    val keepScreenOn: Boolean = false,
    val multitouchZoomEnabled: Boolean = true,
    val boldTextEnabled: Boolean = false,
    val showNotesInReader: Boolean = true,
)
