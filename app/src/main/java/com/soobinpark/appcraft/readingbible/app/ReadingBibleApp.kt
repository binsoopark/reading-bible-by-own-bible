package com.soobinpark.appcraft.readingbible.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.feature.reader.ReaderRoute
import com.soobinpark.appcraft.readingbible.feature.records.RecordsRoute
import com.soobinpark.appcraft.readingbible.feature.search.SearchRoute
import com.soobinpark.appcraft.readingbible.feature.settings.SettingsRoute

@Composable
fun ReadingBibleApp(
    appViewModel: AppViewModel = viewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Reader) }
    val dataFolderUri by appViewModel.dataFolderUri.collectAsState()
    val readingStyle by appViewModel.readingStyle.collectAsState()
    val bookmarks by appViewModel.bookmarks.collectAsState()
    val cacheWarmUpState by appViewModel.cacheWarmUpState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            AppTab.Reader -> ReaderRoute(
                dataFolderUri = dataFolderUri,
                readingStyle = readingStyle,
                bookmarks = bookmarks,
                cacheWarmUpState = cacheWarmUpState,
                onBookmarkToggle = appViewModel::toggleBookmark,
                onHighlightToggle = { verse, color -> appViewModel.toggleHighlight(verse, color) },
                onReadToggle = appViewModel::toggleRead,
                onFontSizeChanged = appViewModel::setReadingFontSize,
                modifier = modifier,
            )
            AppTab.Search -> SearchRoute(dataFolderUri = dataFolderUri, modifier = modifier)
            AppTab.Records -> RecordsRoute(
                bookmarks = bookmarks,
                onNoteChanged = appViewModel::updateBookmarkNote,
                modifier = modifier,
            )
            AppTab.Settings -> SettingsRoute(
                dataFolderUri = dataFolderUri,
                readingStyle = readingStyle,
                onDataFolderSelected = {
                    appViewModel.setDataFolderUri(it)
                    selectedTab = AppTab.Reader
                },
                onFontSizeChanged = appViewModel::setReadingFontSize,
                onLineHeightChanged = appViewModel::setReadingLineHeight,
                onPaletteSelected = appViewModel::setReadingPalette,
                onExportRecords = appViewModel::exportRecordsJson,
                onImportRecords = appViewModel::importRecordsJson,
                modifier = modifier,
            )
        }
    }
}
