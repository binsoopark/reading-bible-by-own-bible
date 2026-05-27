package com.soobinpark.appcraft.readingbible.app

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soobinpark.appcraft.readingbible.data.preference.BookmarkPreferences
import com.soobinpark.appcraft.readingbible.data.preference.DataFolderPreferences
import com.soobinpark.appcraft.readingbible.data.preference.ReadingProgressPreferences
import com.soobinpark.appcraft.readingbible.data.preference.ReadingStylePreferences
import com.soobinpark.appcraft.readingbible.data.repository.FileBibleRepository
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

data class CacheWarmUpUiState(
    val isActive: Boolean = false,
    val message: String = "",
    val progress: Float? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dataFolderPreferences = DataFolderPreferences(application)
    private val readingStylePreferences = ReadingStylePreferences(application)
    private val bookmarkPreferences = BookmarkPreferences(application)
    private val readingProgressPreferences = ReadingProgressPreferences(application)
    private val repository = FileBibleRepository(application)
    private val _dataFolderUri = MutableStateFlow(dataFolderPreferences.getTreeUri())
    private val _readingStyle = MutableStateFlow(readingStylePreferences.getStyle())
    private val _bookmarks = MutableStateFlow(bookmarkPreferences.getBookmarks())
    private val _cacheWarmUpState = MutableStateFlow(CacheWarmUpUiState())
    val dataFolderUri: StateFlow<Uri?> = _dataFolderUri.asStateFlow()
    val readingStyle: StateFlow<ReadingStyle> = _readingStyle.asStateFlow()
    val bookmarks: StateFlow<List<VerseBookmark>> = _bookmarks.asStateFlow()
    val cacheWarmUpState: StateFlow<CacheWarmUpUiState> = _cacheWarmUpState.asStateFlow()

    init {
        warmUpBibleCache(_dataFolderUri.value)
    }

    fun exportRecordsJson(): String = bookmarkPreferences.toJson(_bookmarks.value)

    fun importRecordsJson(json: String) {
        val imported = bookmarkPreferences.fromJson(json)
        val merged = (imported + _bookmarks.value)
            .distinctBy { it.key }
            .sortedByDescending { it.createdAtMillis }
        bookmarkPreferences.setBookmarks(merged)
        _bookmarks.value = merged
    }

    fun setDataFolderUri(uri: Uri?) {
        dataFolderPreferences.setTreeUri(uri)
        _dataFolderUri.value = uri
        warmUpBibleCache(uri)
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

    fun setKeepScreenOn(enabled: Boolean) {
        setReadingStyle(_readingStyle.value.copy(keepScreenOn = enabled))
    }

    fun setMultitouchZoomEnabled(enabled: Boolean) {
        setReadingStyle(_readingStyle.value.copy(multitouchZoomEnabled = enabled))
    }

    fun setBoldTextEnabled(enabled: Boolean) {
        setReadingStyle(_readingStyle.value.copy(boldTextEnabled = enabled))
    }

    fun setShowNotesInReader(enabled: Boolean) {
        setReadingStyle(_readingStyle.value.copy(showNotesInReader = enabled))
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

    fun toggleHighlight(
        verse: BibleVerse,
        color: VerseHighlight = VerseHighlight.Yellow,
    ) {
        val key = VerseBookmark.key(verse.versionCode, verse.bookIndex, verse.chapter, verse.verse)
        val current = _bookmarks.value
        val existing = current.firstOrNull { it.key == key }
        val next = if (existing != null) {
            val updated = existing.copy(
                highlight = if (existing.highlight == VerseHighlight.None) color else VerseHighlight.None,
            )
            current.replaceOrRemoveEmpty(updated)
        } else {
            listOf(verse.toBookmark(isBookmarked = false, highlight = color)) + current
        }
        bookmarkPreferences.setBookmarks(next)
        _bookmarks.value = next
    }

    fun toggleRead(verse: BibleVerse) {
        val key = VerseBookmark.key(verse.versionCode, verse.bookIndex, verse.chapter, verse.verse)
        val current = _bookmarks.value
        val existing = current.firstOrNull { it.key == key }
        val next = if (existing != null) {
            val updated = existing.copy(isRead = !existing.isRead)
            current.replaceOrRemoveEmpty(updated)
        } else {
            listOf(verse.toBookmark(isBookmarked = false, isRead = true)) + current
        }
        bookmarkPreferences.setBookmarks(next)
        _bookmarks.value = next
    }

    private fun BibleVerse.toBookmark(
        isBookmarked: Boolean,
        highlight: VerseHighlight = VerseHighlight.None,
        isRead: Boolean = false,
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
            isRead = isRead,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    private fun List<VerseBookmark>.replaceOrRemoveEmpty(updated: VerseBookmark): List<VerseBookmark> {
        val keep = updated.isBookmarked || updated.note.isNotBlank() || updated.highlight != VerseHighlight.None || updated.isRead
        return if (keep) {
            map { bookmark -> if (bookmark.key == updated.key) updated else bookmark }
        } else {
            filterNot { it.key == updated.key }
        }
    }

    private fun warmUpBibleCache(uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _cacheWarmUpState.value = CacheWarmUpUiState(
                    isActive = true,
                    message = "성경 데이터 폴더를 확인하는 중입니다.",
                )
                val root = File(Environment.getExternalStorageDirectory(), "bible")
                val versions = uri?.let { repository.scanVersions(it) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: repository.scanVersions(root)
                if (versions.isEmpty()) {
                    _cacheWarmUpState.value = CacheWarmUpUiState()
                    return@launch
                }
                val progress = readingProgressPreferences.getProgress()
                val bookIndex = progress.bookIndex.coerceIn(BibleCatalog.books.indices)
                val chapter = progress.chapter.coerceIn(1, BibleCatalog.books[bookIndex].chapterCount)
                val selected = progress.versionCode?.let { code ->
                    versions.firstOrNull { it.code.equals(code, ignoreCase = true) }
                } ?: versions.firstOrNull()
                val book = BibleCatalog.books[bookIndex]
                selected?.let {
                    _cacheWarmUpState.value = CacheWarmUpUiState(
                        isActive = true,
                        message = "마지막으로 읽던 ${book.koreanName} ${chapter}장을 준비하는 중입니다.",
                    )
                    repository.readChapter(it, book, chapter)
                }
                versions.firstOrNull { it.code != selected?.code }?.let {
                    _cacheWarmUpState.value = CacheWarmUpUiState(
                        isActive = true,
                        message = "비교 역본 본문을 미리 읽는 중입니다.",
                    )
                    repository.readChapter(it, book, chapter)
                }
                selected?.let { version ->
                    repository.warmUpVersion(version) { current, total, label ->
                        _cacheWarmUpState.value = CacheWarmUpUiState(
                            isActive = true,
                            message = "${version.displayName} 역본을 빠른 실행용 DB 캐시로 준비 중입니다. $label",
                            progress = current.toFloat() / total.toFloat(),
                        )
                    }
                }
            } finally {
                _cacheWarmUpState.value = CacheWarmUpUiState()
            }
        }
    }
}
