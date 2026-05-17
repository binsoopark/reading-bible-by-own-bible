package com.soobinpark.appcraft.readingbible.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.soobinpark.appcraft.readingbible.data.preference.BookmarkPreferences
import com.soobinpark.appcraft.readingbible.data.preference.DataFolderPreferences
import com.soobinpark.appcraft.readingbible.data.preference.ReadingStylePreferences
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dataFolderPreferences = DataFolderPreferences(application)
    private val readingStylePreferences = ReadingStylePreferences(application)
    private val bookmarkPreferences = BookmarkPreferences(application)
    private val _dataFolderUri = MutableStateFlow(dataFolderPreferences.getTreeUri())
    private val _readingStyle = MutableStateFlow(readingStylePreferences.getStyle())
    private val _bookmarks = MutableStateFlow(bookmarkPreferences.getBookmarks())
    val dataFolderUri: StateFlow<Uri?> = _dataFolderUri.asStateFlow()
    val readingStyle: StateFlow<ReadingStyle> = _readingStyle.asStateFlow()
    val bookmarks: StateFlow<List<VerseBookmark>> = _bookmarks.asStateFlow()

    fun setDataFolderUri(uri: Uri?) {
        dataFolderPreferences.setTreeUri(uri)
        _dataFolderUri.value = uri
    }

    fun setReadingFontSize(sizeSp: Float) {
        setReadingStyle(_readingStyle.value.copy(fontSizeSp = sizeSp))
    }

    fun setReadingLineHeight(multiplier: Float) {
        setReadingStyle(_readingStyle.value.copy(lineHeightMultiplier = multiplier))
    }

    fun setReadingPalette(palette: ReadingPalette) {
        setReadingStyle(_readingStyle.value.copy(palette = palette))
    }

    private fun setReadingStyle(style: ReadingStyle) {
        readingStylePreferences.setStyle(style)
        _readingStyle.value = style
    }

    fun toggleBookmark(verse: BibleVerse) {
        val key = VerseBookmark.key(verse.versionCode, verse.bookIndex, verse.chapter, verse.verse)
        val current = _bookmarks.value
        val existing = current.firstOrNull { it.key == key }
        val next = if (existing != null) {
            val updated = existing.copy(isBookmarked = !existing.isBookmarked)
            current.replaceOrRemoveEmpty(updated)
        } else {
            listOf(verse.toBookmark(isBookmarked = true)) + current
        }
        bookmarkPreferences.setBookmarks(next)
        _bookmarks.value = next
    }

    fun updateBookmarkNote(
        bookmarkKey: String,
        note: String,
    ) {
        val next = _bookmarks.value.map { bookmark ->
            if (bookmark.key == bookmarkKey) bookmark.copy(note = note) else bookmark
        }
        bookmarkPreferences.setBookmarks(next)
        _bookmarks.value = next
    }

    fun toggleHighlight(verse: BibleVerse) {
        val key = VerseBookmark.key(verse.versionCode, verse.bookIndex, verse.chapter, verse.verse)
        val current = _bookmarks.value
        val existing = current.firstOrNull { it.key == key }
        val next = if (existing != null) {
            val updated = existing.copy(
                highlight = if (existing.highlight == VerseHighlight.None) VerseHighlight.Yellow else VerseHighlight.None,
            )
            current.replaceOrRemoveEmpty(updated)
        } else {
            listOf(verse.toBookmark(isBookmarked = false, highlight = VerseHighlight.Yellow)) + current
        }
        bookmarkPreferences.setBookmarks(next)
        _bookmarks.value = next
    }

    private fun BibleVerse.toBookmark(
        isBookmarked: Boolean,
        highlight: VerseHighlight = VerseHighlight.None,
    ): VerseBookmark {
        return VerseBookmark(
            versionCode = versionCode,
            bookIndex = bookIndex,
            chapter = chapter,
            verse = verse,
            text = text,
            note = "",
            isBookmarked = isBookmarked,
            highlight = highlight,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    private fun List<VerseBookmark>.replaceOrRemoveEmpty(updated: VerseBookmark): List<VerseBookmark> {
        val keep = updated.isBookmarked || updated.note.isNotBlank() || updated.highlight != VerseHighlight.None
        return if (keep) {
            map { bookmark -> if (bookmark.key == updated.key) updated else bookmark }
        } else {
            filterNot { it.key == updated.key }
        }
    }
}
