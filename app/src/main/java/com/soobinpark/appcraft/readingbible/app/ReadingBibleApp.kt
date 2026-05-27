package com.soobinpark.appcraft.readingbible.app

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.core.design.ReadingBibleTheme
import com.soobinpark.appcraft.readingbible.feature.reader.ReaderRoute
import com.soobinpark.appcraft.readingbible.feature.reader.ReaderViewModel
import com.soobinpark.appcraft.readingbible.feature.records.RecordsRoute
import com.soobinpark.appcraft.readingbible.feature.search.SearchRoute
import com.soobinpark.appcraft.readingbible.feature.settings.SettingsRoute

@Composable
fun ReadingBibleApp(
    appViewModel: AppViewModel = viewModel(),
    readerViewModel: ReaderViewModel = viewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Reader) }
    val dataFolderUri by appViewModel.dataFolderUri.collectAsState()
    val readingStyle by appViewModel.readingStyle.collectAsState()
    val bookmarks by appViewModel.bookmarks.collectAsState()
    val cacheWarmUpState by appViewModel.cacheWarmUpState.collectAsState()

    ReadingBibleTheme(palette = readingStyle.palette) {
        val view = LocalView.current
        DisposableEffect(readingStyle.keepScreenOn) {
            val window = (view.context as? Activity)?.window
            if (readingStyle.keepScreenOn) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

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
                    viewModel = readerViewModel,
                )
                AppTab.Search -> SearchRoute(
                    dataFolderUri = dataFolderUri,
                    onResultLongPressed = { result ->
                        readerViewModel.openSearchResult(result.verse)
                        selectedTab = AppTab.Reader
                    },
                    modifier = modifier,
                )
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
                    onKeepScreenOnChanged = appViewModel::setKeepScreenOn,
                    onMultitouchZoomEnabledChanged = appViewModel::setMultitouchZoomEnabled,
                    onBoldTextEnabledChanged = appViewModel::setBoldTextEnabled,
                    onShowNotesInReaderChanged = appViewModel::setShowNotesInReader,
                    onExportRecords = appViewModel::exportRecordsJson,
                    onImportRecords = appViewModel::importRecordsJson,
                    modifier = modifier,
                )
            }
        }
    }
}
