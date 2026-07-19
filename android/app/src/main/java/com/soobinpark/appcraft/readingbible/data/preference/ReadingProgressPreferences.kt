package com.soobinpark.appcraft.readingbible.data.preference

import android.content.Context
import com.soobinpark.appcraft.readingbible.domain.model.ReadingProgress

class ReadingProgressPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reading_bible_preferences", Context.MODE_PRIVATE)

    fun getProgress(): ReadingProgress {
        return ReadingProgress(
            versionCode = prefs.getString(KEY_VERSION_CODE, null),
            comparisonVersionCode = prefs.getString(KEY_COMPARISON_VERSION_CODE, null),
            hasSavedComparisonVersion = prefs.getBoolean(KEY_COMPARISON_VERSION_SAVED, false),
            bookIndex = prefs.getInt(KEY_BOOK_INDEX, 0),
            chapter = prefs.getInt(KEY_CHAPTER, 1),
        )
    }

    fun setProgress(progress: ReadingProgress) {
        prefs.edit()
            .putString(KEY_VERSION_CODE, progress.versionCode)
            .putString(KEY_COMPARISON_VERSION_CODE, progress.comparisonVersionCode)
            .putBoolean(KEY_COMPARISON_VERSION_SAVED, progress.hasSavedComparisonVersion)
            .putInt(KEY_BOOK_INDEX, progress.bookIndex)
            .putInt(KEY_CHAPTER, progress.chapter)
            .apply()
    }

    private companion object {
        const val KEY_VERSION_CODE = "last_version_code"
        const val KEY_COMPARISON_VERSION_CODE = "last_comparison_version_code"
        const val KEY_COMPARISON_VERSION_SAVED = "has_saved_comparison_version"
        const val KEY_BOOK_INDEX = "last_book_index"
        const val KEY_CHAPTER = "last_chapter"
    }
}
