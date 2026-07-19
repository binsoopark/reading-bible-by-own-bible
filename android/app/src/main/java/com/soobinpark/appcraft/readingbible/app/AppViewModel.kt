package com.soobinpark.appcraft.readingbible.app

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soobinpark.appcraft.readingbible.data.preference.BookmarkPreferences
import com.soobinpark.appcraft.readingbible.data.preference.DataFolderPreferences
import com.soobinpark.appcraft.readingbible.data.preference.PersonalNotePreferences
import com.soobinpark.appcraft.readingbible.data.preference.ReadingProgressPreferences
import com.soobinpark.appcraft.readingbible.data.preference.ReadingStylePreferences
import com.soobinpark.appcraft.readingbible.data.repository.FileBibleRepository
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.PersonalNote
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import com.soobinpark.appcraft.readingbible.domain.model.shouldPersist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class CacheWarmUpUiState(
    val isActive: Boolean = false,
    val message: String = "",
    val progress: Float? = null,
)

data class BibleDataDownloadUiState(
    val isActive: Boolean = false,
    val message: String = "",
    val progress: Float? = null,
    val installedRoot: File? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dataFolderPreferences = DataFolderPreferences(application)
    private val readingStylePreferences = ReadingStylePreferences(application)
    private val bookmarkPreferences = BookmarkPreferences(application)
    private val readingProgressPreferences = ReadingProgressPreferences(application)
    private val personalNotePreferences = PersonalNotePreferences(application)
    private val repository = FileBibleRepository(application)
    private val _dataFolderUri = MutableStateFlow(dataFolderPreferences.getTreeUri())
    private val _localDataRoot = MutableStateFlow(dataFolderPreferences.getLocalRoot())
    private val _readingStyle = MutableStateFlow(readingStylePreferences.getStyle())
    private val _bookmarks = MutableStateFlow(bookmarkPreferences.getBookmarks())
    private val _personalNotes = MutableStateFlow(personalNotePreferences.getNotes())
    private val _cacheWarmUpState = MutableStateFlow(CacheWarmUpUiState())
    private val _dataDownloadState = MutableStateFlow(
        BibleDataDownloadUiState(installedRoot = dataFolderPreferences.getLocalRoot()),
    )
    val dataFolderUri: StateFlow<Uri?> = _dataFolderUri.asStateFlow()
    val localDataRoot: StateFlow<File?> = _localDataRoot.asStateFlow()
    val readingStyle: StateFlow<ReadingStyle> = _readingStyle.asStateFlow()
    val bookmarks: StateFlow<List<VerseBookmark>> = _bookmarks.asStateFlow()
    val personalNotes: StateFlow<List<PersonalNote>> = _personalNotes.asStateFlow()
    val cacheWarmUpState: StateFlow<CacheWarmUpUiState> = _cacheWarmUpState.asStateFlow()
    val dataDownloadState: StateFlow<BibleDataDownloadUiState> = _dataDownloadState.asStateFlow()

    init {
        warmUpBibleCache(_dataFolderUri.value, _localDataRoot.value)
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
        _localDataRoot.value = null
        _dataDownloadState.value = _dataDownloadState.value.copy(installedRoot = null)
        warmUpBibleCache(uri, null)
    }

    fun downloadSampleBibleData() {
        if (_dataDownloadState.value.isActive) return
        viewModelScope.launch(Dispatchers.IO) {
            if (hasExistingBibleData()) {
                _dataDownloadState.value = BibleDataDownloadUiState(
                    isActive = false,
                    message = "이미 성경 데이터가 있습니다. 다운로드는 데이터가 없을 때만 이용할 수 있습니다.",
                    installedRoot = _localDataRoot.value,
                )
                return@launch
            }
            val application = getApplication<Application>()
            val downloadFile = File(application.cacheDir, "bible.zip")
            val installBase = File(application.getExternalFilesDir(null) ?: application.filesDir, "downloaded-bible")
            val stageDir = File(installBase.parentFile ?: application.filesDir, "downloaded-bible-stage")
            try {
                _dataDownloadState.value = BibleDataDownloadUiState(
                    isActive = true,
                    message = "성경 데이터 파일을 다운로드하는 중입니다.",
                    progress = 0f,
                    installedRoot = _localDataRoot.value,
                )
                downloadFile.parentFile?.mkdirs()
                downloadFile(downloadFile)
                _dataDownloadState.value = BibleDataDownloadUiState(
                    isActive = true,
                    message = "다운로드 파일을 확인하는 중입니다.",
                    progress = 0.72f,
                    installedRoot = _localDataRoot.value,
                )
                check(downloadFile.sha256() == BibleZipSha256) { "다운로드한 파일의 체크섬이 일치하지 않습니다." }

                stageDir.deleteRecursively()
                stageDir.mkdirs()
                _dataDownloadState.value = BibleDataDownloadUiState(
                    isActive = true,
                    message = "성경 데이터 압축을 해제하는 중입니다.",
                    progress = 0.82f,
                    installedRoot = _localDataRoot.value,
                )
                unzipBibleData(downloadFile, stageDir)
                installBase.deleteRecursively()
                check(stageDir.renameTo(installBase)) { "압축 해제한 폴더를 적용하지 못했습니다." }
                val root = File(installBase, "bible").takeIf { it.isDirectory } ?: installBase
                dataFolderPreferences.setLocalRoot(root)
                _dataFolderUri.value = null
                _localDataRoot.value = root
                _dataDownloadState.value = BibleDataDownloadUiState(
                    isActive = false,
                    message = "성경 데이터를 다운로드하고 적용했습니다.",
                    progress = 1f,
                    installedRoot = root,
                )
                warmUpBibleCache(null, root)
            } catch (error: Throwable) {
                _dataDownloadState.value = BibleDataDownloadUiState(
                    isActive = false,
                    message = "성경 데이터 다운로드에 실패했습니다. ${error.message.orEmpty()}",
                    installedRoot = _localDataRoot.value,
                )
            } finally {
                downloadFile.delete()
                stageDir.deleteRecursively()
            }
        }
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
        val next = _bookmarks.value.mapNotNull { bookmark ->
            if (bookmark.key != bookmarkKey) {
                bookmark
            } else {
                val updated = bookmark.copy(
                    note = note,
                    createdAtMillis = System.currentTimeMillis(),
                )
                updated.takeIf { it.shouldPersist() }
            }
        }
        bookmarkPreferences.setBookmarks(next)
        _bookmarks.value = next
    }

    fun updateVerseNote(verse: BibleVerse, note: String) {
        val key = VerseBookmark.key(verse.versionCode, verse.bookIndex, verse.chapter, verse.verse)
        val current = _bookmarks.value
        val existing = current.firstOrNull { it.key == key }
        val next = if (existing != null) {
            current.replaceOrRemoveEmpty(
                existing.copy(
                    note = note,
                    text = verse.text,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        } else if (note.isNotBlank()) {
            listOf(verse.toBookmark(isBookmarked = false).copy(note = note)) + current
        } else {
            current
        }
        bookmarkPreferences.setBookmarks(next)
        _bookmarks.value = next
    }

    fun addPersonalNote(title: String, body: String) {
        val note = PersonalNote.create(title = title, body = body)
        val next = listOf(note) + _personalNotes.value
        personalNotePreferences.setNotes(next)
        _personalNotes.value = next
    }

    fun updatePersonalNote(note: PersonalNote) {
        val next = _personalNotes.value
            .map { existing -> if (existing.id == note.id) note.copy(updatedAtMillis = System.currentTimeMillis()) else existing }
            .sortedByDescending { it.updatedAtMillis }
        personalNotePreferences.setNotes(next)
        _personalNotes.value = next
    }

    fun deletePersonalNote(id: String) {
        val next = _personalNotes.value.filterNot { it.id == id }
        personalNotePreferences.setNotes(next)
        _personalNotes.value = next
    }

    fun exportPersonalNotes(ids: Set<String>): String {
        val selected = _personalNotes.value.filter { ids.contains(it.id) }
        return personalNotePreferences.exportBackup(selected)
    }

    fun importPersonalNotes(raw: String) {
        val imported = personalNotePreferences.importBackup(raw)
        val merged = (_personalNotes.value + imported)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.updatedAtMillis }
        personalNotePreferences.setNotes(merged.toList())
        _personalNotes.value = merged.toList()
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

    fun setHighlights(
        verses: List<BibleVerse>,
        color: VerseHighlight,
    ) {
        if (verses.isEmpty()) return
        var next = _bookmarks.value
        val now = System.currentTimeMillis()
        verses.forEach { verse ->
            val key = VerseBookmark.key(verse.versionCode, verse.bookIndex, verse.chapter, verse.verse)
            val existing = next.firstOrNull { it.key == key }
            next = if (existing != null) {
                next.replaceOrRemoveEmpty(existing.copy(highlight = color, createdAtMillis = now))
            } else if (color != VerseHighlight.None) {
                listOf(verse.toBookmark(isBookmarked = false, highlight = color)) + next
            } else {
                next
            }
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
        val keep = updated.shouldPersist()
        return if (keep) {
            map { bookmark -> if (bookmark.key == updated.key) updated else bookmark }
        } else {
            filterNot { it.key == updated.key }
        }
    }

    private fun warmUpBibleCache(
        uri: Uri?,
        localRoot: File?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _cacheWarmUpState.value = CacheWarmUpUiState(
                    isActive = true,
                    message = "성경 데이터 폴더를 확인하는 중입니다.",
                )
                val root = File(Environment.getExternalStorageDirectory(), "bible")
                val versions = uri?.let { repository.scanVersions(it) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: localRoot?.let { repository.scanVersions(it) }?.takeIf { it.isNotEmpty() }
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

    private suspend fun hasExistingBibleData(): Boolean {
        _dataFolderUri.value?.let { uri ->
            if (repository.scanVersions(uri).isNotEmpty()) return true
        }
        _localDataRoot.value?.let { root ->
            if (repository.scanVersions(root).isNotEmpty()) return true
        }
        dataFolderPreferences.getLocalRoot()?.let { root ->
            if (root.exists() && repository.scanVersions(root).isNotEmpty()) return true
        }
        val fallback = File(Environment.getExternalStorageDirectory(), "bible")
        if (fallback.exists() && repository.scanVersions(fallback).isNotEmpty()) return true
        return false
    }

    private fun downloadFile(destination: File) {
        val connection = (URL(BibleZipUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            val total = connection.contentLengthLong.takeIf { it > 0L } ?: BibleZipSizeBytes
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        _dataDownloadState.value = BibleDataDownloadUiState(
                            isActive = true,
                            message = "성경 데이터 파일을 다운로드하는 중입니다.",
                            progress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 0.7f),
                            installedRoot = _localDataRoot.value,
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun unzipBibleData(
        zipFile: File,
        destination: File,
    ) {
        val canonicalDestination = destination.canonicalFile
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val target = File(destination, entry.name).canonicalFile
                check(target.path == canonicalDestination.path || target.path.startsWith(canonicalDestination.path + File.separator)) {
                    "안전하지 않은 압축 경로입니다."
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } >= 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val BibleZipUrl = "https://github.com/binsoopark/reading-bible-by-own-bible/releases/download/bible-data-2026.05.31/bible.zip"
        const val BibleZipSha256 = "071576aeac0514b4010ddc71e86cafd9e24ab3a47876415cf359d5ccd8c4ddc4"
        const val BibleZipSizeBytes = 50_929_083L
    }
}
