package com.soobinpark.appcraft.readingbible.feature.reader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark

@Composable
fun ReaderRoute(
    dataFolderUri: Uri?,
    readingStyle: ReadingStyle,
    bookmarks: List<VerseBookmark>,
    onBookmarkToggle: (BibleVerse) -> Unit,
    onHighlightToggle: (BibleVerse) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(dataFolderUri) {
        viewModel.setDataFolderUri(dataFolderUri)
    }
    ReaderScreen(
        state = uiState,
        readingStyle = readingStyle,
        bookmarks = bookmarks,
        onVersionSelected = viewModel::selectVersion,
        onBookChapterSelected = viewModel::selectBookAndChapter,
        onBookmarkToggle = onBookmarkToggle,
        onHighlightToggle = onHighlightToggle,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    state: ReaderUiState,
    readingStyle: ReadingStyle,
    bookmarks: List<VerseBookmark>,
    onVersionSelected: (BibleVersion) -> Unit,
    onBookChapterSelected: (Int, Int) -> Unit,
    onBookmarkToggle: (BibleVerse) -> Unit,
    onHighlightToggle: (BibleVerse) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showVersionSheet by remember { mutableStateOf(false) }
    var showBookSheet by remember { mutableStateOf(false) }
    val currentBook = BibleCatalog.books[state.bookIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "내가 가진 성경 읽기",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { showVersionSheet = true },
                label = { Text(state.selectedVersion?.code ?: "역본 없음") },
            )
            AssistChip(
                onClick = { showBookSheet = true },
                label = { Text("${currentBook.koreanName} ${state.chapter}장") },
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        } else if (state.message != null) {
            EmptyReaderState(state)
        } else {
            val palette = readingPaletteColors(readingStyle.palette)
            val recordsByKey = bookmarks.associateBy { it.key }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.verses, key = { "${it.versionCode}-${it.bookIndex}-${it.chapter}-${it.verse}" }) { verse ->
                    val record = recordsByKey[verse.bookmarkKey()]
                    val isHighlighted = record?.highlight != null && record.highlight != VerseHighlight.None
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHighlighted) palette.highlightContainer else palette.container,
                            contentColor = palette.content,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${verse.verse}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.accent,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { onHighlightToggle(verse) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.FormatColorFill,
                                        contentDescription = if (isHighlighted) "하이라이트 해제" else "하이라이트 추가",
                                        tint = if (isHighlighted) palette.highlightAccent else palette.accent,
                                    )
                                }
                                IconButton(onClick = { onBookmarkToggle(verse) }) {
                                    val selected = record?.isBookmarked == true
                                    Icon(
                                        imageVector = if (selected) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = if (selected) "북마크 해제" else "북마크 추가",
                                        tint = palette.accent,
                                    )
                                }
                            }
                            Text(
                                text = verse.text,
                                fontSize = readingStyle.fontSizeSp.sp,
                                lineHeight = (readingStyle.fontSizeSp * readingStyle.lineHeightMultiplier).sp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVersionSheet) {
        VersionPickerSheet(
            versions = state.versions,
            selectedVersion = state.selectedVersion,
            onDismiss = { showVersionSheet = false },
            onVersionSelected = {
                onVersionSelected(it)
                showVersionSheet = false
            },
        )
    }

    if (showBookSheet) {
        BookChapterPickerSheet(
            selectedBookIndex = state.bookIndex,
            selectedChapter = state.chapter,
            onDismiss = { showBookSheet = false },
            onBookChapterSelected = { bookIndex, chapter ->
                onBookChapterSelected(bookIndex, chapter)
                showBookSheet = false
            },
        )
    }
}

private fun BibleVerse.bookmarkKey(): String {
    return VerseBookmark.key(versionCode, bookIndex, chapter, verse)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionPickerSheet(
    versions: List<BibleVersion>,
    selectedVersion: BibleVersion?,
    onDismiss: () -> Unit,
    onVersionSelected: (BibleVersion) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleVersions = remember(versions, query) {
        versions
            .filter { version ->
                query.isBlank() ||
                    version.code.contains(query, ignoreCase = true) ||
                    version.displayName.contains(query, ignoreCase = true) ||
                    version.sourceType.name.contains(query, ignoreCase = true)
            }
            .sortedWith(compareBy({ it.sourceType.name }, { it.code.lowercase() }))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("역본 선택", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("역본 검색") },
                placeholder = { Text("예: kor, niv, bdf") },
            )
            if (visibleVersions.isEmpty()) {
                Text("검색 결과가 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleVersions, key = { "${it.sourceType}-${it.code}" }) { version ->
                        FilterChip(
                            selected = selectedVersion == version,
                            onClick = { onVersionSelected(version) },
                            label = { Text("${version.code} · ${version.sourceType}") },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookChapterPickerSheet(
    selectedBookIndex: Int,
    selectedChapter: Int,
    onDismiss: () -> Unit,
    onBookChapterSelected: (Int, Int) -> Unit,
) {
    var activeBookIndex by remember(selectedBookIndex) { mutableStateOf(selectedBookIndex) }
    val activeBook = BibleCatalog.books[activeBookIndex]
    val chapters = remember(activeBookIndex) { (1..activeBook.chapterCount).toList() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("책과 장 선택", style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier.heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(BibleCatalog.books, key = { it.index }) { book ->
                    FilterChip(
                        selected = book.index == activeBookIndex,
                        onClick = { activeBookIndex = book.index },
                        label = { Text("${book.koreanName} · ${book.englishName}") },
                    )
                }
            }
            Text(
                text = "${activeBook.koreanName} ${activeBook.chapterCount}장",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier = Modifier.heightIn(max = 320.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chapters, key = { it }) { chapter ->
                    FilterChip(
                        selected = activeBookIndex == selectedBookIndex && chapter == selectedChapter,
                        onClick = { onBookChapterSelected(activeBookIndex, chapter) },
                        label = { Text("${chapter}장") },
                    )
                }
            }
        }
    }
}

private data class ReaderPaletteColors(
    val container: Color,
    val content: Color,
    val accent: Color,
    val highlightContainer: Color,
    val highlightAccent: Color,
)

private fun readingPaletteColors(palette: ReadingPalette): ReaderPaletteColors {
    return when (palette) {
        ReadingPalette.Paper -> ReaderPaletteColors(
            container = Color(0xFFFFFCF4),
            content = Color(0xFF242018),
            accent = Color(0xFF1B6B62),
            highlightContainer = Color(0xFFFFF2A8),
            highlightAccent = Color(0xFF8A6200),
        )
        ReadingPalette.Evening -> ReaderPaletteColors(
            container = Color(0xFF26231F),
            content = Color(0xFFF1E8D8),
            accent = Color(0xFFE0B56B),
            highlightContainer = Color(0xFF4A3B1D),
            highlightAccent = Color(0xFFFFD66E),
        )
        ReadingPalette.Oled -> ReaderPaletteColors(
            container = Color(0xFF000000),
            content = Color(0xFFECECEC),
            accent = Color(0xFF77D7C8),
            highlightContainer = Color(0xFF242000),
            highlightAccent = Color(0xFFFFE45E),
        )
        ReadingPalette.HighContrast -> ReaderPaletteColors(
            container = Color(0xFFFFFFFF),
            content = Color(0xFF000000),
            accent = Color(0xFF005BD3),
            highlightContainer = Color(0xFFFFFF66),
            highlightAccent = Color(0xFF000000),
        )
        ReadingPalette.WarmLight -> ReaderPaletteColors(
            container = Color(0xFFFFF1D6),
            content = Color(0xFF2D2215),
            accent = Color(0xFF9A5B00),
            highlightContainer = Color(0xFFFFDFA0),
            highlightAccent = Color(0xFF855200),
        )
    }
}

@Composable
private fun EmptyReaderState(state: ReaderUiState) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("성경 데이터가 필요합니다", style = MaterialTheme.typography.titleMedium)
            Text(state.message.orEmpty())
            Text("현재 확인한 폴더: ${state.dataRoot.absolutePath}")
            Text("지원 포맷: .bdf, .lfa, .lfb")
        }
    }
}
