package com.soobinpark.appcraft.readingbible.app

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val localDataRoot by appViewModel.localDataRoot.collectAsState()
    val readingStyle by appViewModel.readingStyle.collectAsState()
    val bookmarks by appViewModel.bookmarks.collectAsState()
    val cacheWarmUpState by appViewModel.cacheWarmUpState.collectAsState()
    val dataDownloadState by appViewModel.dataDownloadState.collectAsState()

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

        Scaffold { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                val modifier = Modifier.padding(innerPadding)
                when (selectedTab) {
                    AppTab.Reader -> ReaderRoute(
                        dataFolderUri = dataFolderUri,
                        localDataRoot = localDataRoot,
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
                        localDataRoot = localDataRoot,
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
                        localDataRoot = localDataRoot,
                        dataDownloadState = dataDownloadState,
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
                        onDownloadBibleData = appViewModel::downloadSampleBibleData,
                        modifier = modifier,
                    )
                }
                BottomContentFade(Modifier.align(Alignment.BottomCenter))
                FloatingBottomTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun BottomContentFade(
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(136.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        background.copy(alpha = 0.62f),
                        background,
                    ),
                ),
            ),
    )
}

@Composable
private fun FloatingBottomTabs(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 58.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 296.dp)
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 14.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTab.entries.forEach { tab ->
                    FloatingBottomTabItem(
                        tab = tab,
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomTabItem(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
        },
        label = "floating-tab-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "floating-tab-content",
    )
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = tab.label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
