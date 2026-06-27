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
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE_SP, 18f).coerceIn(12f, 28f),
            lineHeightMultiplier = prefs.getFloat(KEY_LINE_HEIGHT_MULTIPLIER, 1.55f).coerceIn(1.2f, 2.2f),
            palette = palette,
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false),
            multitouchZoomEnabled = prefs.getBoolean(KEY_MULTITOUCH_ZOOM_ENABLED, true),
            boldTextEnabled = prefs.getBoolean(KEY_BOLD_TEXT_ENABLED, false),
            showNotesInReader = prefs.getBoolean(KEY_SHOW_NOTES_IN_READER, true),
        )
    }

    fun setStyle(style: ReadingStyle) {
        prefs.edit()
            .putFloat(KEY_FONT_SIZE_SP, style.fontSizeSp.coerceIn(12f, 28f))
            .putFloat(KEY_LINE_HEIGHT_MULTIPLIER, style.lineHeightMultiplier.coerceIn(1.2f, 2.2f))
            .putString(KEY_PALETTE, style.palette.name)
            .putBoolean(KEY_KEEP_SCREEN_ON, style.keepScreenOn)
            .putBoolean(KEY_MULTITOUCH_ZOOM_ENABLED, style.multitouchZoomEnabled)
            .putBoolean(KEY_BOLD_TEXT_ENABLED, style.boldTextEnabled)
            .putBoolean(KEY_SHOW_NOTES_IN_READER, style.showNotesInReader)
            .apply()
    }

    private companion object {
        const val KEY_FONT_SIZE_SP = "reading_font_size_sp"
        const val KEY_LINE_HEIGHT_MULTIPLIER = "reading_line_height_multiplier"
        const val KEY_PALETTE = "reading_palette"
        const val KEY_KEEP_SCREEN_ON = "reading_keep_screen_on"
        const val KEY_MULTITOUCH_ZOOM_ENABLED = "reading_multitouch_zoom_enabled"
        const val KEY_BOLD_TEXT_ENABLED = "reading_bold_text_enabled"
        const val KEY_SHOW_NOTES_IN_READER = "reading_show_notes_in_reader"
    }
}
