package com.soobinpark.appcraft.readingbible.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.soobinpark.appcraft.readingbible.data.preference.DataFolderPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = DataFolderPreferences(application)
    private val _dataFolderUri = MutableStateFlow(preferences.getTreeUri())
    val dataFolderUri: StateFlow<Uri?> = _dataFolderUri.asStateFlow()

    fun setDataFolderUri(uri: Uri?) {
        preferences.setTreeUri(uri)
        _dataFolderUri.value = uri
    }
}
