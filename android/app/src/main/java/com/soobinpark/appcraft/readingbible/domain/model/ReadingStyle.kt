package com.soobinpark.appcraft.readingbible.domain.model

enum class ReadingPalette(
    val label: String,
) {
    Paper("종이"),
    Evening("저녁"),
    Oled("OLED"),
    HighContrast("고대비"),
    WarmLight("따뜻한 빛"),
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
