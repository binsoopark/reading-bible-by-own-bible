package com.soobinpark.appcraft.readingbible.feature.search

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soobinpark.appcraft.readingbible.data.repository.FileBibleRepository
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.usecase.SearchBibleUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class SearchUiState(
    val dataRoot: File = File(Environment.getExternalStorageDirectory(), "bible"),
    val versions: List<BibleVersion> = emptyList(),
    val selectedVersion: BibleVersion? = null,
    val query: String = "",
    val results: List<BibleSearchResult> = emptyList(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val message: String? = null,
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileBibleRepository(application)
    private val searchBible = SearchBibleUseCase(repository)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var dataFolderUri: Uri? = null
    private var searchJob: Job? = null

    init {
        refreshVersions()
    }

    fun setDataFolderUri(uri: Uri?) {
        if (dataFolderUri == uri) return
        dataFolderUri = uri
        refreshVersions()
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun selectVersion(version: BibleVersion) {
        _uiState.value = _uiState.value.copy(selectedVersion = version, results = emptyList(), message = null)
    }

    fun refreshVersions() {
        viewModelScope.launch {
            val root = _uiState.value.dataRoot
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            val versions = dataFolderUri?.let { repository.scanVersions(it) }
                ?.takeIf { it.isNotEmpty() }
                ?: repository.scanVersions(root)
            _uiState.value = _uiState.value.copy(
                versions = versions,
                selectedVersion = versions.firstOrNull(),
                results = emptyList(),
                isLoading = false,
                message = if (versions.isEmpty()) "검색할 bdf/lfa 역본을 찾지 못했습니다. 설정 탭에서 데이터 폴더를 선택해 주세요." else null,
            )
        }
    }

    fun search() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val state = _uiState.value
            val version = state.selectedVersion
            val query = state.query.trim()
            if (version == null) {
                _uiState.value = state.copy(message = "검색할 역본이 없습니다.")
                return@launch
            }
            if (query.length < 2) {
                _uiState.value = state.copy(results = emptyList(), message = "검색어를 두 글자 이상 입력해 주세요.")
                return@launch
            }

            _uiState.value = state.copy(isSearching = true, message = null, results = emptyList())
            val results = searchBible(version = version, query = query)
            _uiState.value = _uiState.value.copy(
                results = results,
                isSearching = false,
                message = if (results.isEmpty()) "검색 결과가 없습니다." else null,
            )
        }
    }
}
