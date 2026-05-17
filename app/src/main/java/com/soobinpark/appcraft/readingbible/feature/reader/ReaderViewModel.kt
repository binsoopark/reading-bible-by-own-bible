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

    init {
        refresh()
    }

    fun setDataFolderUri(uri: Uri?) {
        if (dataFolderUri == uri) return
        dataFolderUri = uri
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
                    ?: repository.scanVersions(root)
            }
            val savedProgress = progressPreferences.getProgress()
            val safeBookIndex = savedProgress.bookIndex.coerceIn(BibleCatalog.books.indices)
            val safeChapter = savedProgress.chapter.coerceIn(1, BibleCatalog.books[safeBookIndex].chapterCount)
            val selected = savedProgress.versionCode?.let { code ->
                versions.firstOrNull { it.code.equals(code, ignoreCase = true) }
            } ?: versions.firstOrNull()
            val comparison = versions.firstOrNull { it.code != selected?.code }
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
                message = if (versions.isEmpty()) "선택한 폴더 또는 기본 데이터 폴더에서 bdf/lfa 파일을 찾지 못했습니다." else null,
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
            )
            saveProgress(version, state.bookIndex, state.chapter)
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
                verses = verses,
                comparisonVerses = comparisonVerses,
            )
            saveProgress(state.selectedVersion, safeBookIndex, safeChapter)
        }
    }

    private fun warmUpSelectedVersion(version: BibleVersion) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.warmUpVersion(version)
        }
    }

    private fun saveProgress(
        version: BibleVersion?,
        bookIndex: Int,
        chapter: Int,
    ) {
        progressPreferences.setProgress(
            ReadingProgress(
                versionCode = version?.code,
                bookIndex = bookIndex,
                chapter = chapter,
            ),
        )
    }
}
