package com.soobinpark.appcraft.readingbible.feature.reader

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.app.CacheWarmUpUiState
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReaderRoute(
    dataFolderUri: Uri?,
    readingStyle: ReadingStyle,
    bookmarks: List<VerseBookmark>,
    cacheWarmUpState: CacheWarmUpUiState,
    onBookmarkToggle: (BibleVerse) -> Unit,
    onHighlightToggle: (BibleVerse) -> Unit,
    onReadToggle: (BibleVerse) -> Unit,
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
        cacheWarmUpState = cacheWarmUpState,
        onVersionSelected = viewModel::selectVersion,
        onComparisonVersionSelected = viewModel::selectComparisonVersion,
        onBookChapterSelected = viewModel::selectBookAndChapter,
        onBookmarkToggle = onBookmarkToggle,
        onHighlightToggle = onHighlightToggle,
        onReadToggle = onReadToggle,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    state: ReaderUiState,
    readingStyle: ReadingStyle,
    bookmarks: List<VerseBookmark>,
    cacheWarmUpState: CacheWarmUpUiState,
    onVersionSelected: (BibleVersion) -> Unit,
    onComparisonVersionSelected: (BibleVersion?) -> Unit,
    onBookChapterSelected: (Int, Int) -> Unit,
    onBookmarkToggle: (BibleVerse) -> Unit,
    onHighlightToggle: (BibleVerse) -> Unit,
    onReadToggle: (BibleVerse) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showVersionSheet by remember { mutableStateOf(false) }
    var showComparisonSheet by remember { mutableStateOf(false) }
    var showBookSheet by remember { mutableStateOf(false) }
    var showVerseSheet by remember { mutableStateOf(false) }
    var selectedVerseNumber by remember(state.bookIndex, state.chapter) { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentBook = BibleCatalog.books[state.bookIndex]
    val previousChapter = remember(state.bookIndex, state.chapter) {
        adjacentChapter(state.bookIndex, state.chapter, direction = -1)
    }
    val nextChapter = remember(state.bookIndex, state.chapter) {
        adjacentChapter(state.bookIndex, state.chapter, direction = 1)
    }

    LaunchedEffect(state.bookIndex, state.chapter, selectedVerseNumber) {
        listState.scrollToItem(0)
    }

    fun moveTo(target: ChapterTarget?) {
        if (target == null) return
        selectedVerseNumber = null
        onBookChapterSelected(target.bookIndex, target.chapter)
        coroutineScope.launch {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactIconButton(
                onClick = { moveTo(previousChapter) },
                enabled = previousChapter != null && !state.isLoading,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "이전 장")
            }
            SuggestionChip(
                onClick = { showBookSheet = true },
                label = { Text("${currentBook.koreanName} ${state.chapter}장") },
                modifier = Modifier.weight(1f),
            )
            CompactIconButton(
                onClick = { moveTo(nextChapter) },
                enabled = nextChapter != null && !state.isLoading,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "다음 장")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuggestionChip(
                onClick = { showVersionSheet = true },
                label = { Text(state.selectedVersion?.displayName ?: "역본 없음") },
                modifier = Modifier.weight(1f),
            )
            SuggestionChip(
                onClick = { showComparisonSheet = true },
                label = { Text(state.comparisonVersion?.let { "비교 ${it.displayName}" } ?: "비교 없음") },
                modifier = Modifier.weight(1f),
            )
            SuggestionChip(
                onClick = { showVerseSheet = true },
                label = { Text(selectedVerseNumber?.let { "${it}절" } ?: "전체 절") },
            )
        }
        if (cacheWarmUpState.isActive) {
            CacheWarmUpBanner(cacheWarmUpState)
        }

        if (state.isLoading) {
            LoadingReaderState(state.loadingMessage)
        } else if (state.message != null) {
            EmptyReaderState(state)
        } else {
            val palette = readingPaletteColors(readingStyle.palette)
            val recordsByKey = bookmarks.associateBy { it.key }
            val comparisonByVerse = state.comparisonVerses.associateBy { it.verse }
            val visibleVerses = selectedVerseNumber?.let { verseNumber ->
                state.verses.filter { it.verse == verseNumber }
            } ?: state.verses
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.bookIndex, state.chapter, previousChapter, nextChapter) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                when {
                                    totalDrag < -120f -> moveTo(nextChapter)
                                    totalDrag > 120f -> moveTo(previousChapter)
                                }
                            },
                        )
                    },
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(end = 30.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibleVerses, key = { "${it.versionCode}-${it.bookIndex}-${it.chapter}-${it.verse}" }) { verse ->
                        val record = recordsByKey[verse.bookmarkKey()]
                        val comparisonVerse = comparisonByVerse[verse.verse]
                        val isHighlighted = record?.highlight != null && record.highlight != VerseHighlight.None
                        val isRead = record?.isRead == true
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
                                    IconButton(onClick = { onReadToggle(verse) }) {
                                        Icon(
                                            imageVector = if (isRead) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = if (isRead) "읽음 해제" else "읽음 표시",
                                            tint = if (isRead) palette.highlightAccent else palette.accent,
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
                                if (comparisonVerse != null) {
                                    Text(
                                        text = comparisonVerse.text,
                                        color = palette.secondaryContent,
                                        fontSize = (readingStyle.fontSizeSp - 1f).coerceAtLeast(12f).sp,
                                        lineHeight = (readingStyle.fontSizeSp * readingStyle.lineHeightMultiplier).sp,
                                        modifier = Modifier.padding(top = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                FastVerseScrollBar(
                    listState = listState,
                    itemCount = visibleVerses.size,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }

    if (showVersionSheet) {
        VersionPickerSheet(
            versions = state.versions,
            selectedVersion = state.selectedVersion,
            onDismiss = { showVersionSheet = false },
            allowClear = false,
            onVersionSelected = {
                onVersionSelected(it)
                showVersionSheet = false
            },
        )
    }

    if (showComparisonSheet) {
        VersionPickerSheet(
            versions = state.versions.filter { it.code != state.selectedVersion?.code },
            selectedVersion = state.comparisonVersion,
            onDismiss = { showComparisonSheet = false },
            allowClear = true,
            onCleared = {
                onComparisonVersionSelected(null)
                showComparisonSheet = false
            },
            onVersionSelected = {
                onComparisonVersionSelected(it)
                showComparisonSheet = false
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

    if (showVerseSheet) {
        VersePickerSheet(
            verses = state.verses.map { it.verse },
            selectedVerse = selectedVerseNumber,
            onDismiss = { showVerseSheet = false },
            onVerseSelected = {
                selectedVerseNumber = it
                showVerseSheet = false
            },
            onShowAll = {
                selectedVerseNumber = null
                showVerseSheet = false
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
    allowClear: Boolean,
    onCleared: () -> Unit = {},
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
            .sortedWith(compareBy({ it.sourceType.name }, { it.displayName.lowercase() }, { it.code.lowercase() }))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("역본 선택", style = MaterialTheme.typography.titleLarge)
            if (allowClear) {
                TextButton(onClick = onCleared) {
                    Text("비교 역본 끄기")
                }
            }
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
                            label = {
                                Column {
                                    Text(version.displayName)
                                    Text(
                                        text = "${version.code} · ${version.sourceType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
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
    var testament by remember(selectedBookIndex) { mutableStateOf(if (selectedBookIndex < 39) "구약" else "신약") }
    val activeBook = BibleCatalog.books[activeBookIndex]
    val chapters = remember(activeBookIndex) { (1..activeBook.chapterCount).toList() }
    val visibleBooks = remember(testament) {
        if (testament == "구약") BibleCatalog.books.take(39) else BibleCatalog.books.drop(39)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("책과 장 선택", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = testament == "구약",
                    onClick = { testament = "구약" },
                    label = { Text("구약") },
                )
                FilterChip(
                    selected = testament == "신약",
                    onClick = { testament = "신약" },
                    label = { Text("신약") },
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 92.dp),
                modifier = Modifier.heightIn(max = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleBooks, key = { it.index }) { book ->
                    FilterChip(
                        selected = book.index == activeBookIndex,
                        onClick = { activeBookIndex = book.index },
                        label = { Text(book.koreanName) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersePickerSheet(
    verses: List<Int>,
    selectedVerse: Int?,
    onDismiss: () -> Unit,
    onVerseSelected: (Int) -> Unit,
    onShowAll: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("절 선택", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onShowAll) {
                Text("전체 절 보기")
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier = Modifier.heightIn(max = 360.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(verses, key = { it }) { verse ->
                    FilterChip(
                        selected = selectedVerse == verse,
                        onClick = { onVerseSelected(verse) },
                        label = { Text("${verse}절") },
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
    val secondaryContent: Color,
)

private fun readingPaletteColors(palette: ReadingPalette): ReaderPaletteColors {
    return when (palette) {
        ReadingPalette.Paper -> ReaderPaletteColors(
            container = Color(0xFFFFFCF4),
            content = Color(0xFF242018),
            accent = Color(0xFF1B6B62),
            highlightContainer = Color(0xFFFFF2A8),
            highlightAccent = Color(0xFF8A6200),
            secondaryContent = Color(0xFF625B4B),
        )
        ReadingPalette.Evening -> ReaderPaletteColors(
            container = Color(0xFF26231F),
            content = Color(0xFFF1E8D8),
            accent = Color(0xFFE0B56B),
            highlightContainer = Color(0xFF4A3B1D),
            highlightAccent = Color(0xFFFFD66E),
            secondaryContent = Color(0xFFCDBFA8),
        )
        ReadingPalette.Oled -> ReaderPaletteColors(
            container = Color(0xFF000000),
            content = Color(0xFFECECEC),
            accent = Color(0xFF77D7C8),
            highlightContainer = Color(0xFF242000),
            highlightAccent = Color(0xFFFFE45E),
            secondaryContent = Color(0xFFB8B8B8),
        )
        ReadingPalette.HighContrast -> ReaderPaletteColors(
            container = Color(0xFFFFFFFF),
            content = Color(0xFF000000),
            accent = Color(0xFF005BD3),
            highlightContainer = Color(0xFFFFFF66),
            highlightAccent = Color(0xFF000000),
            secondaryContent = Color(0xFF262626),
        )
        ReadingPalette.WarmLight -> ReaderPaletteColors(
            container = Color(0xFFFFF1D6),
            content = Color(0xFF2D2215),
            accent = Color(0xFF9A5B00),
            highlightContainer = Color(0xFFFFDFA0),
            highlightAccent = Color(0xFF855200),
            secondaryContent = Color(0xFF6D4F2C),
        )
    }
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    size: Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        ),
        modifier = Modifier.size(size),
        content = content,
    )
}

private data class ChapterTarget(
    val bookIndex: Int,
    val chapter: Int,
)

private fun adjacentChapter(
    bookIndex: Int,
    chapter: Int,
    direction: Int,
): ChapterTarget? {
    val book = BibleCatalog.books.getOrNull(bookIndex) ?: return null
    return when {
        direction > 0 && chapter < book.chapterCount -> ChapterTarget(bookIndex, chapter + 1)
        direction > 0 && bookIndex < BibleCatalog.books.lastIndex -> ChapterTarget(bookIndex + 1, 1)
        direction < 0 && chapter > 1 -> ChapterTarget(bookIndex, chapter - 1)
        direction < 0 && bookIndex > 0 -> {
            val previousBook = BibleCatalog.books[bookIndex - 1]
            ChapterTarget(bookIndex - 1, previousBook.chapterCount)
        }
        else -> null
    }
}

@Composable
private fun FastVerseScrollBar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    val visibleCount by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) }
    }
    if (itemCount <= 1) return

    val trackHeightPx = trackSize.height.toFloat().coerceAtLeast(1f)
    val thumbHeightPx = (trackHeightPx * visibleCount / itemCount.toFloat())
        .coerceIn(with(density) { 36.dp.toPx() }, trackHeightPx)
    val maxFirstIndex = (itemCount - visibleCount).coerceAtLeast(1)
    val firstIndex = listState.firstVisibleItemIndex.coerceIn(0, maxFirstIndex)
    val thumbOffsetPx = ((trackHeightPx - thumbHeightPx) * firstIndex / maxFirstIndex).coerceIn(0f, trackHeightPx - thumbHeightPx)

    fun scrollToPosition(y: Float) {
        if (trackSize.height <= 0) return
        val movable = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
        val ratio = ((y - thumbHeightPx / 2f) / movable).coerceIn(0f, 1f)
        val targetIndex = (ratio * maxFirstIndex).roundToInt().coerceIn(0, itemCount - 1)
        coroutineScope.launch {
            listState.scrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxSize()
            .padding(vertical = 2.dp)
            .onSizeChanged { trackSize = it }
            .pointerInput(itemCount, trackSize) {
                detectVerticalDragGestures(
                    onDragStart = { offset: Offset -> scrollToPosition(offset.y) },
                    onVerticalDrag = { change, _ -> scrollToPosition(change.position.y) },
                )
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(99.dp),
                ),
        )
        if (trackSize.height > 0) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = thumbOffsetPx.roundToInt()) }
                    .width(12.dp)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(99.dp),
                    ),
            )
        }
    }
}

@Composable
private fun LoadingReaderState(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = message.ifBlank { "성경 데이터를 준비하는 중입니다." },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "처음 여는 데이터는 BDF/LFA 파일을 읽고 앱 내부 캐시에 저장합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CacheWarmUpBanner(state: CacheWarmUpUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
            state.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } ?: Spacer(modifier = Modifier.fillMaxWidth())
        }
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
