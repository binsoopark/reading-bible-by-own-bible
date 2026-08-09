package com.soobinpark.appcraft.readingbible.feature.reader

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soobinpark.appcraft.readingbible.data.preference.ReadingProgressPreferences
import com.soobinpark.appcraft.readingbible.data.repository.FileBibleRepository
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.model.ReadingProgress
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReaderUiState(
    val dataRoot: File = File(Environment.getExternalStorageDirectory(), "bible"),
    val versions: List<BibleVersion> = emptyList(),
    val selectedVersion: BibleVersion? = null,
    val comparisonVersion: BibleVersion? = null,
    val bookIndex: Int = 0,
    val chapter: Int = 1,
    val focusVerse: Int? = null,
    val verses: List<BibleVerse> = emptyList(),
    val comparisonVerses: List<BibleVerse> = emptyList(),
    val isLoading: Boolean = true,
    val loadingMessage: String = "성경 데이터를 준비하는 중입니다.",
    val message: String? = null,
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileBibleRepository(application)
    private val progressPreferences = ReadingProgressPreferences(application)
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    private var dataFolderUri: Uri? = null
    private var localDataRoot: File? = null

    init {
        refresh()
    }

    fun setDataFolderUri(uri: Uri?) {
        if (dataFolderUri == uri) return
        dataFolderUri = uri
        localDataRoot = null
        refresh()
    }

    fun setDataFolder(
        uri: Uri?,
        localRoot: File?,
    ) {
        if (dataFolderUri == uri && localDataRoot == localRoot) return
        dataFolderUri = uri
        localDataRoot = localRoot
        _uiState.value = _uiState.value.copy(dataRoot = localRoot ?: File(Environment.getExternalStorageDirectory(), "bible"))
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val root = _uiState.value.dataRoot
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "데이터 폴더에서 BDF/LFA 역본을 찾는 중입니다.",
                message = null,
            )
            val versions = withContext(Dispatchers.IO) {
                dataFolderUri?.let { repository.scanVersions(it) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: localDataRoot?.let { repository.scanVersions(it) }?.takeIf { it.isNotEmpty() }
                    ?: repository.scanVersions(root)
            }
            val savedProgress = progressPreferences.getProgress()
            val safeBookIndex = savedProgress.bookIndex.coerceIn(BibleCatalog.books.indices)
            val safeChapter = savedProgress.chapter.coerceIn(1, BibleCatalog.books[safeBookIndex].chapterCount)
            val selected = savedProgress.versionCode?.let { code ->
                versions.firstOrNull { it.code.equals(code, ignoreCase = true) }
            } ?: versions.firstOrNull()
            val comparison = if (savedProgress.hasSavedComparisonVersion) {
                savedProgress.comparisonVersionCode?.let { code ->
                    versions.firstOrNull {
                        it.code.equals(code, ignoreCase = true) && it.code != selected?.code
                    }
                }
            } else {
                versions.firstOrNull { it.code != selected?.code }
            }
            val book = BibleCatalog.books[safeBookIndex]
            _uiState.value = _uiState.value.copy(
                versions = versions,
                selectedVersion = selected,
                comparisonVersion = comparison,
                bookIndex = safeBookIndex,
                chapter = safeChapter,
                loadingMessage = selected?.let { "${it.code} ${book.koreanName} ${safeChapter}장을 여는 중입니다." }
                    ?: "사용 가능한 역본을 확인하는 중입니다.",
            )
            val (verses, comparisonVerses) = withContext(Dispatchers.IO) {
                val verses = selected?.let { repository.readChapter(it, book, safeChapter) }.orEmpty()
                val comparisonVerses = comparison?.let { repository.readChapter(it, book, safeChapter) }.orEmpty()
                verses to comparisonVerses
            }
            _uiState.value = _uiState.value.copy(
                verses = verses,
                comparisonVerses = comparisonVerses,
                isLoading = false,
                loadingMessage = "",
                message = when {
                    versions.isEmpty() -> "선택한 폴더 또는 기본 데이터 폴더에서 bdf/lfa 파일을 찾지 못했습니다."
                    selected != null && verses.isEmpty() -> "${selected.displayName} ${book.koreanName} ${safeChapter}장을 읽지 못했습니다. LFA/BDF 파일 형식 또는 손상 여부를 확인해 주세요."
                    else -> null
                },
            )
            _uiState.value.selectedVersion?.let { warmUpSelectedVersion(it) }
        }
    }

    fun selectVersion(version: BibleVersion) {
        viewModelScope.launch {
            val state = _uiState.value
            val (verses, comparison, comparisonVerses) = withContext(Dispatchers.IO) {
                val verses = repository.readChapter(version, BibleCatalog.books[state.bookIndex], state.chapter)
                val comparison = state.comparisonVersion?.takeUnless { it.code == version.code }
                val comparisonVerses = comparison?.let {
                    repository.readChapter(it, BibleCatalog.books[state.bookIndex], state.chapter)
                }.orEmpty()
                Triple(verses, comparison, comparisonVerses)
            }
            _uiState.value = state.copy(
                selectedVersion = version,
                comparisonVersion = comparison,
                verses = verses,
                comparisonVerses = comparisonVerses,
                message = if (verses.isEmpty()) {
                    val book = BibleCatalog.books[state.bookIndex]
                    "${version.displayName} ${book.koreanName} ${state.chapter}장을 읽지 못했습니다. LFA/BDF 파일 형식 또는 손상 여부를 확인해 주세요."
                } else {
                    null
                },
            )
            saveProgress(version, comparison, state.bookIndex, state.chapter)
            warmUpSelectedVersion(version)
        }
    }

    fun selectComparisonVersion(version: BibleVersion?) {
        viewModelScope.launch {
            val state = _uiState.value
            val comparison = version?.takeUnless { it.code == state.selectedVersion?.code }
            val verses = withContext(Dispatchers.IO) {
                comparison?.let {
                    repository.readChapter(it, BibleCatalog.books[state.bookIndex], state.chapter)
                }.orEmpty()
            }
            _uiState.value = state.copy(comparisonVersion = comparison, comparisonVerses = verses)
            saveProgress(state.selectedVersion, comparison, state.bookIndex, state.chapter)
        }
    }

    fun selectBookAndChapter(bookIndex: Int, chapter: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val safeBookIndex = bookIndex.coerceIn(BibleCatalog.books.indices)
            val safeChapter = chapter.coerceIn(1, BibleCatalog.books[safeBookIndex].chapterCount)
            val (verses, comparisonVerses) = withContext(Dispatchers.IO) {
                val verses = state.selectedVersion?.let {
                    repository.readChapter(it, BibleCatalog.books[safeBookIndex], safeChapter)
                }.orEmpty()
                val comparisonVerses = state.comparisonVersion?.let {
                    repository.readChapter(it, BibleCatalog.books[safeBookIndex], safeChapter)
                }.orEmpty()
                verses to comparisonVerses
            }
            _uiState.value = state.copy(
                bookIndex = safeBookIndex,
                chapter = safeChapter,
                focusVerse = null,
                verses = verses,
                comparisonVerses = comparisonVerses,
                message = if (state.selectedVersion != null && verses.isEmpty()) {
                    val book = BibleCatalog.books[safeBookIndex]
                    "${state.selectedVersion.displayName} ${book.koreanName} ${safeChapter}장을 읽지 못했습니다. LFA/BDF 파일 형식 또는 손상 여부를 확인해 주세요."
                } else {
                    null
                },
            )
            saveProgress(state.selectedVersion, state.comparisonVersion, safeBookIndex, safeChapter)
        }
    }

    fun openSearchResult(verse: BibleVerse) {
        viewModelScope.launch {
            val state = _uiState.value
            val versions = if (state.versions.isNotEmpty()) {
                state.versions
            } else {
                withContext(Dispatchers.IO) {
                    dataFolderUri?.let { repository.scanVersions(it) }
                        ?.takeIf { it.isNotEmpty() }
                        ?: localDataRoot?.let { repository.scanVersions(it) }?.takeIf { it.isNotEmpty() }
                        ?: repository.scanVersions(state.dataRoot)
                }
            }
            val safeBookIndex = verse.bookIndex.coerceIn(BibleCatalog.books.indices)
            val safeChapter = verse.chapter.coerceIn(1, BibleCatalog.books[safeBookIndex].chapterCount)
            val targetVersion = versions.firstOrNull { it.code.equals(verse.versionCode, ignoreCase = true) }
                ?: state.selectedVersion
                ?: versions.firstOrNull()
            val comparison = state.comparisonVersion
                ?.takeUnless { it.code == targetVersion?.code }
            val (verses, comparisonVerses) = withContext(Dispatchers.IO) {
                val targetVerses = targetVersion?.let {
                    repository.readChapter(it, BibleCatalog.books[safeBookIndex], safeChapter)
                }.orEmpty()
                val targetComparisonVerses = comparison?.let {
                    repository.readChapter(it, BibleCatalog.books[safeBookIndex], safeChapter)
                }.orEmpty()
                targetVerses to targetComparisonVerses
            }
            _uiState.value = state.copy(
                versions = versions,
                selectedVersion = targetVersion,
                comparisonVersion = comparison,
                bookIndex = safeBookIndex,
                chapter = safeChapter,
                focusVerse = verse.verse,
                verses = verses,
                comparisonVerses = comparisonVerses,
                isLoading = false,
                loadingMessage = "",
                message = if (targetVersion != null && verses.isEmpty()) {
                    val book = BibleCatalog.books[safeBookIndex]
                    "${targetVersion.displayName} ${book.koreanName} ${safeChapter}장을 읽지 못했습니다. LFA/BDF 파일 형식 또는 손상 여부를 확인해 주세요."
                } else {
                    null
                },
            )
            saveProgress(targetVersion, comparison, safeBookIndex, safeChapter)
            targetVersion?.let { warmUpSelectedVersion(it) }
        }
    }

    fun openBookmark(bookmark: VerseBookmark) {
        openSearchResult(
            BibleVerse(
                versionCode = bookmark.versionCode,
                bookIndex = bookmark.bookIndex,
                chapter = bookmark.chapter,
                verse = bookmark.verse,
                text = bookmark.text,
            ),
        )
    }

    private fun warmUpSelectedVersion(version: BibleVersion) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.warmUpVersion(version)
        }
    }

    private fun saveProgress(
        version: BibleVersion?,
        comparisonVersion: BibleVersion?,
        bookIndex: Int,
        chapter: Int,
    ) {
        progressPreferences.setProgress(
            ReadingProgress(
                versionCode = version?.code,
                comparisonVersionCode = comparisonVersion?.code,
                hasSavedComparisonVersion = true,
                bookIndex = bookIndex,
                chapter = chapter,
            ),
        )
    }
}
