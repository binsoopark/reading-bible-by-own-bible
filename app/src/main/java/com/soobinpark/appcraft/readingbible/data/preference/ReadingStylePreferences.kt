package com.soobinpark.appcraft.readingbible.data.preference

import android.content.Context
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle

class ReadingStylePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reading_bible_preferences", Context.MODE_PRIVATE)

    fun getStyle(): ReadingStyle {
        val palette = prefs.getString(KEY_PALETTE, ReadingPalette.Paper.name)
            ?.let { runCatching { ReadingPalette.valueOf(it) }.getOrNull() }
            ?: ReadingPalette.Paper
        return ReadingStyle(
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE_SP, 18f).coerceIn(14f, 28f),
            lineHeightMultiplier = prefs.getFloat(KEY_LINE_HEIGHT_MULTIPLIER, 1.55f).coerceIn(1.2f, 2.2f),
            palette = palette,
        )
    }

    fun setStyle(style: ReadingStyle) {
        prefs.edit()
            .putFloat(KEY_FONT_SIZE_SP, style.fontSizeSp.coerceIn(14f, 28f))
            .putFloat(KEY_LINE_HEIGHT_MULTIPLIER, style.lineHeightMultiplier.coerceIn(1.2f, 2.2f))
            .putString(KEY_PALETTE, style.palette.name)
            .apply()
    }

    private companion object {
        const val KEY_FONT_SIZE_SP = "reading_font_size_sp"
        const val KEY_LINE_HEIGHT_MULTIPLIER = "reading_line_height_multiplier"
        const val KEY_PALETTE = "reading_palette"
    }
}
