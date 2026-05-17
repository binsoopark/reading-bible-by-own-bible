package com.soobinpark.appcraft.readingbible.feature.reader

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soobinpark.appcraft.readingbible.data.repository.FileBibleRepository
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ReaderUiState(
    val dataRoot: File = File(Environment.getExternalStorageDirectory(), "bible"),
    val versions: List<BibleVersion> = emptyList(),
    val selectedVersion: BibleVersion? = null,
    val bookIndex: Int = 0,
    val chapter: Int = 1,
    val verses: List<BibleVerse> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileBibleRepository(application)
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
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            val versions = dataFolderUri?.let { repository.scanVersions(it) }
                ?.takeIf { it.isNotEmpty() }
                ?: repository.scanVersions(root)
            val selected = versions.firstOrNull()
            val verses = selected?.let {
                repository.readChapter(it, BibleCatalog.books[_uiState.value.bookIndex], _uiState.value.chapter)
            }.orEmpty()
            _uiState.value = _uiState.value.copy(
                versions = versions,
                selectedVersion = selected,
                verses = verses,
                isLoading = false,
                message = if (versions.isEmpty()) "선택한 폴더 또는 기본 데이터 폴더에서 bdf/lfa 파일을 찾지 못했습니다." else null,
            )
        }
    }

    fun selectVersion(version: BibleVersion) {
        viewModelScope.launch {
            val state = _uiState.value
            val verses = repository.readChapter(version, BibleCatalog.books[state.bookIndex], state.chapter)
            _uiState.value = state.copy(selectedVersion = version, verses = verses)
        }
    }

    fun selectBookAndChapter(bookIndex: Int, chapter: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val safeBookIndex = bookIndex.coerceIn(BibleCatalog.books.indices)
            val safeChapter = chapter.coerceIn(1, BibleCatalog.books[safeBookIndex].chapterCount)
            val verses = state.selectedVersion?.let {
                repository.readChapter(it, BibleCatalog.books[safeBookIndex], safeChapter)
            }.orEmpty()
            _uiState.value = state.copy(bookIndex = safeBookIndex, chapter = safeChapter, verses = verses)
        }
    }
}
