package com.soobinpark.appcraft.readingbible.feature.library

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soobinpark.appcraft.readingbible.data.biblefile.BibleFileDiagnosticsReader
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileDiagnostic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class LibraryUiState(
    val dataRoot: File = File(Environment.getExternalStorageDirectory(), "bible"),
    val dataFolderUri: Uri? = null,
    val diagnostic: BibleFileDiagnostic? = null,
    val isLoading: Boolean = true,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val diagnosticsReader = BibleFileDiagnosticsReader(application)
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setDataFolderUri(uri: Uri?) {
        if (_uiState.value.dataFolderUri == uri) return
        _uiState.value = _uiState.value.copy(dataFolderUri = uri)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = true)
            val diagnostic = state.dataFolderUri?.let { diagnosticsReader.diagnose(it) }
                ?: diagnosticsReader.diagnose(state.dataRoot)
            _uiState.value = state.copy(
                diagnostic = diagnostic,
                isLoading = false,
            )
        }
    }
}
