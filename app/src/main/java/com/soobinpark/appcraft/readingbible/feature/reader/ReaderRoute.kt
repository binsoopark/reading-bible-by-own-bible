package com.soobinpark.appcraft.readingbible.feature.reader

import android.net.Uri
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ReaderRoute(
    dataFolderUri: Uri?,
    readingStyle: ReadingStyle,
    bookmarks: List<VerseBookmark>,
    cacheWarmUpState: CacheWarmUpUiState,
    onBookmarkToggle: (BibleVerse) -> Unit,
    onHighlightToggle: (BibleVerse, VerseHighlight) -> Unit,
    onReadToggle: (BibleVerse) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
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
        onFontSizeChanged = onFontSizeChanged,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    onHighlightToggle: (BibleVerse, VerseHighlight) -> Unit,
    onReadToggle: (BibleVerse) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showVersionSheet by remember { mutableStateOf(false) }
    var showComparisonSheet by remember { mutableStateOf(false) }
    var showBookSheet by remember { mutableStateOf(false) }
    var showHighlightColorSheet by remember { mutableStateOf(false) }
    var selectedHighlightColor by remember { mutableStateOf(VerseHighlight.Yellow) }
    var chapterSlideDirection by remember { mutableStateOf(AnimatedContentTransitionScope.SlideDirection.Left) }
    val context = LocalContext.current
    var fontSizeToast by remember { mutableStateOf<Toast?>(null) }
    var selectedVerseKeys by remember(state.bookIndex, state.chapter) { mutableStateOf(setOf<String>()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentBook = BibleCatalog.books[state.bookIndex]
    val previousChapter = remember(state.bookIndex, state.chapter) {
        adjacentChapter(state.bookIndex, state.chapter, direction = -1)
    }
    val nextChapter = remember(state.bookIndex, state.chapter) {
        adjacentChapter(state.bookIndex, state.chapter, direction = 1)
    }

    LaunchedEffect(state.bookIndex, state.chapter, state.focusVerse, state.verses) {
        val focusIndex = state.focusVerse?.let { focusVerse ->
            state.verses.indexOfFirst { it.verse == focusVerse }.takeIf { it >= 0 }
        }
        listState.scrollToItem(focusIndex ?: 0)
    }

    fun moveTo(target: ChapterTarget?) {
        if (target == null) return
        chapterSlideDirection = if (target.bookIndex > state.bookIndex || target.chapter > state.chapter) {
            AnimatedContentTransitionScope.SlideDirection.Left
        } else {
            AnimatedContentTransitionScope.SlideDirection.Right
        }
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
            val visibleVerses = state.verses
            val selectedVerses = visibleVerses.filter { selectedVerseKeys.contains(it.bookmarkKey()) }
            val effectiveLineHeightMultiplier = dynamicReaderLineHeight(readingStyle.fontSizeSp, readingStyle.lineHeightMultiplier)
            val verseSpacing = dynamicVerseSpacing(readingStyle.fontSizeSp)
            val cardPadding = dynamicVersePadding(readingStyle.fontSizeSp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (readingStyle.multitouchZoomEnabled) {
                            Modifier.readerPinchZoom(readingStyle.fontSizeSp) { fontSize ->
                                val message = "글자 크기 ${fontSize.roundToInt()}"
                                val toast = fontSizeToast ?: Toast.makeText(context, message, Toast.LENGTH_SHORT)
                                    .also { fontSizeToast = it }
                                toast.setText(message)
                                toast.show()
                                onFontSizeChanged(fontSize)
                            }
                        } else {
                            Modifier
                        },
                    )
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
                Column(Modifier.fillMaxSize()) {
                    if (selectedVerses.isNotEmpty()) {
                        SelectedVersesBar(
                            count = selectedVerses.size,
                            onClear = { selectedVerseKeys = emptySet() },
                            onCopy = {
                                val text = selectedVerses.selectionText(currentBook.koreanName)
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("성경 구절", text))
                                Toast.makeText(context, "선택한 구절을 복사했습니다", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val text = selectedVerses.selectionText(currentBook.koreanName)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "구절 공유"))
                            },
                        )
                    }
                AnimatedContent(
                    targetState = "${state.bookIndex}:${state.chapter}",
                    transitionSpec = {
                        slideIntoContainer(chapterSlideDirection, tween(180)) togetherWith
                            slideOutOfContainer(chapterSlideDirection, tween(180))
                    },
                    label = "chapter-transition",
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(end = 30.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(verseSpacing),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(visibleVerses, key = { "${it.versionCode}-${it.bookIndex}-${it.chapter}-${it.verse}" }) { verse ->
                            val verseKey = verse.bookmarkKey()
                            val record = recordsByKey[verse.bookmarkKey()]
                            val comparisonVerse = comparisonByVerse[verse.verse]
                            val highlight = record?.highlight ?: VerseHighlight.None
                            val isHighlighted = highlight != VerseHighlight.None
                            val isSelected = selectedVerseKeys.contains(verseKey)
                            val isRead = record?.isRead == true
                            Card(
                                modifier = Modifier
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(12.dp),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedVerseKeys.isNotEmpty()) {
                                                selectedVerseKeys = selectedVerseKeys.toggle(verseKey)
                                            }
                                        },
                                        onLongClick = {
                                            selectedVerseKeys = selectedVerseKeys.toggle(verseKey)
                                        },
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isHighlighted -> highlightContainerColor(highlight)
                                        else -> palette.container
                                    },
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else palette.content,
                                ),
                            ) {
                            Column(Modifier.padding(cardPadding)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${verse.verse}",
                                        fontSize = (readingStyle.fontSizeSp + 2f).coerceIn(16f, 30f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else palette.accent,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (readingStyle.showNotesInReader && record?.note?.isNotBlank() == true) {
                                        Icon(
                                            imageVector = Icons.Outlined.Description,
                                            contentDescription = "메모 있음",
                                            tint = palette.highlightAccent,
                                            modifier = Modifier.size(dynamicActionIconSize(readingStyle.fontSizeSp)),
                                        )
                                    }
                                    ScaledVerseIconButton(
                                        fontSizeSp = readingStyle.fontSizeSp,
                                        onClick = { onHighlightToggle(verse, selectedHighlightColor) },
                                        onLongClick = { showHighlightColorSheet = true },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FormatColorFill,
                                            contentDescription = if (isHighlighted) "하이라이트 해제" else "하이라이트 추가",
                                            tint = if (isHighlighted) highlightAccentColor(highlight) else palette.accent,
                                            modifier = Modifier.size(dynamicActionIconSize(readingStyle.fontSizeSp)),
                                        )
                                    }
                                    ScaledVerseIconButton(
                                        fontSizeSp = readingStyle.fontSizeSp,
                                        onClick = { onReadToggle(verse) },
                                    ) {
                                        Icon(
                                            imageVector = if (isRead) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = if (isRead) "읽음 해제" else "읽음 표시",
                                            tint = if (isRead) palette.highlightAccent else palette.accent,
                                            modifier = Modifier.size(dynamicActionIconSize(readingStyle.fontSizeSp)),
                                        )
                                    }
                                    ScaledVerseIconButton(
                                        fontSizeSp = readingStyle.fontSizeSp,
                                        onClick = { onBookmarkToggle(verse) },
                                    ) {
                                        val selected = record?.isBookmarked == true
                                        Icon(
                                            imageVector = if (selected) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = if (selected) "북마크 해제" else "북마크 추가",
                                            tint = palette.accent,
                                            modifier = Modifier.size(dynamicActionIconSize(readingStyle.fontSizeSp)),
                                        )
                                    }
                                }
                                Text(
                                    text = verse.text,
                                    fontSize = readingStyle.fontSizeSp.sp,
                                    lineHeight = (readingStyle.fontSizeSp * effectiveLineHeightMultiplier).sp,
                                    fontWeight = if (readingStyle.boldTextEnabled) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (comparisonVerse != null) {
                                    Text(
                                        text = comparisonVerse.text,
                                        color = palette.secondaryContent,
                                        fontSize = (readingStyle.fontSizeSp - 1f).coerceAtLeast(12f).sp,
                                        lineHeight = (readingStyle.fontSizeSp * effectiveLineHeightMultiplier).sp,
                                        fontWeight = if (readingStyle.boldTextEnabled) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(top = 10.dp),
                                    )
                                }
                            }
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

    if (showHighlightColorSheet) {
        HighlightColorSheet(
            selected = selectedHighlightColor,
            onDismiss = { showHighlightColorSheet = false },
            onSelected = {
                selectedHighlightColor = it
                showHighlightColorSheet = false
            },
        )
    }
}

private fun BibleVerse.bookmarkKey(): String {
    return VerseBookmark.key(versionCode, bookIndex, chapter, verse)
}

private fun Set<String>.toggle(key: String): Set<String> {
    return if (contains(key)) this - key else this + key
}

private fun List<BibleVerse>.selectionText(bookName: String): String {
    val sorted = sortedWith(compareBy({ it.bookIndex }, { it.chapter }, { it.verse }))
    val first = sorted.firstOrNull() ?: return ""
    val header = "$bookName ${first.chapter}장"
    val body = sorted.joinToString("\n") { verse -> "${verse.verse}절 ${verse.text}" }
    return "$header\n$body"
}

@Composable
private fun SelectedVersesBar(
    count: Int,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${count}개 선택",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text("해제") }
            Button(onClick = onCopy) { Text("복사") }
            Button(onClick = onShare) { Text("공유") }
        }
    }
}

private fun BibleVersion.languageGroup(): String {
    val key = "${code.lowercase()} ${displayName.lowercase()}"
    return when {
        key.contains("kor") || key.contains("개역") || key.contains("한글") || key.contains("한국") -> "한국어"
        key.contains("eng") || key.contains("niv") || key.contains("kjv") || key.contains("nasb") ||
            key.contains("english") -> "영어"
        key.contains("jpn") || key.contains("japanese") || key.contains("일본") -> "일본어"
        key.contains("chn") || key.contains("chi") || key.contains("chinese") || key.contains("중국") -> "중국어"
        key.contains("grk") || key.contains("greek") || key.contains("헬라") -> "원어/그리스어"
        key.contains("heb") || key.contains("hebrew") || key.contains("히브리") -> "원어/히브리어"
        else -> "기타"
    }
}

private object VersionLanguageGroup {
    private val order = listOf("한국어", "영어", "일본어", "중국어", "원어/그리스어", "원어/히브리어", "기타")

    fun orderOf(group: String): Int = order.indexOf(group).takeIf { it >= 0 } ?: order.size
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
    val groupedVersions = remember(versions) {
        versions
            .sortedWith(compareBy({ it.sourceType.name }, { it.displayName.lowercase() }, { it.code.lowercase() }))
            .groupBy { it.languageGroup() }
            .toSortedMap(compareBy { VersionLanguageGroup.orderOf(it) })
    }
    val listState = rememberLazyListState()
    val itemCount = groupedVersions.values.sumOf { it.size } + groupedVersions.keys.size

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
            if (groupedVersions.isEmpty()) {
                Text("선택할 역본이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Box(modifier = Modifier.heightIn(max = 460.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(end = 28.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groupedVersions.forEach { (group, groupVersions) ->
                            item(key = "header-$group") {
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            items(groupVersions, key = { "${it.sourceType}-${it.code}" }) { version ->
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
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    FastVerseScrollBar(
                        listState = listState,
                        itemCount = itemCount,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
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
    val bookListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedBookIndex - 3).coerceAtLeast(0),
    )
    val chapterListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedChapter - 4).coerceAtLeast(0),
    )

    LaunchedEffect(activeBookIndex) {
        val targetChapter = if (activeBookIndex == selectedBookIndex) selectedChapter - 1 else 0
        chapterListState.scrollToItem(targetChapter.coerceAtLeast(0))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("책과 장 선택", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 560.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PickerListPanel(
                    title = "성경",
                    listState = bookListState,
                    itemCount = BibleCatalog.books.size,
                    modifier = Modifier.weight(1.15f),
                ) {
                    items(BibleCatalog.books, key = { it.index }) { book ->
                        FilterChip(
                            selected = book.index == activeBookIndex,
                            onClick = { activeBookIndex = book.index },
                            label = {
                                Column {
                                    Text(book.koreanName)
                                    Text(
                                        text = if (book.index < 39) "구약" else "신약",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                PickerListPanel(
                    title = "${activeBook.koreanName} ${activeBook.chapterCount}장",
                    listState = chapterListState,
                    itemCount = chapters.size,
                    modifier = Modifier.weight(0.85f),
                ) {
                    items(chapters, key = { it }) { chapter ->
                        FilterChip(
                            selected = activeBookIndex == selectedBookIndex && chapter == selectedChapter,
                            onClick = { onBookChapterSelected(activeBookIndex, chapter) },
                            label = { Text("${chapter}장") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerListPanel(
    title: String,
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = 24.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
            FastVerseScrollBar(
                listState = listState,
                itemCount = itemCount,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
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

private fun highlightContainerColor(highlight: VerseHighlight): Color {
    return when (highlight) {
        VerseHighlight.None -> Color.Transparent
        VerseHighlight.Yellow -> Color(0xFFFFF2A8)
        VerseHighlight.Mint -> Color(0xFFD9F7E8)
        VerseHighlight.Blue -> Color(0xFFDCEBFF)
        VerseHighlight.Pink -> Color(0xFFFFE0EA)
        VerseHighlight.Lavender -> Color(0xFFEAE2FF)
        VerseHighlight.Orange -> Color(0xFFFFE3C2)
    }
}

private fun highlightAccentColor(highlight: VerseHighlight): Color {
    return when (highlight) {
        VerseHighlight.None -> Color.Unspecified
        VerseHighlight.Yellow -> Color(0xFF8A6200)
        VerseHighlight.Mint -> Color(0xFF007A4D)
        VerseHighlight.Blue -> Color(0xFF1F64C8)
        VerseHighlight.Pink -> Color(0xFFC73368)
        VerseHighlight.Lavender -> Color(0xFF6C4BD2)
        VerseHighlight.Orange -> Color(0xFFB95D00)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HighlightColorSheet(
    selected: VerseHighlight,
    onDismiss: () -> Unit,
    onSelected: (VerseHighlight) -> Unit,
) {
    val colors = listOf(VerseHighlight.Yellow, VerseHighlight.Mint, VerseHighlight.Blue, VerseHighlight.Pink, VerseHighlight.Lavender, VerseHighlight.Orange)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("하이라이트 색상", style = MaterialTheme.typography.titleLarge)
            colors.forEach { color ->
                FilterChip(
                    selected = selected == color,
                    onClick = { onSelected(color) },
                    label = { Text(color.label) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(highlightContainerColor(color), RoundedCornerShape(99.dp)),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun Modifier.readerPinchZoom(
    currentFontSizeSp: Float,
    onFontSizeChanged: (Float) -> Unit,
): Modifier {
    val currentFontSize by rememberUpdatedState(currentFontSizeSp)
    val onFontSizeChange by rememberUpdatedState(onFontSizeChanged)
    return pointerInput(Unit) {
        var previousDistance: Float? = null
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val first = pressed[0].position
                    val second = pressed[1].position
                    val distance = pointerDistance(first, second)
                    val previous = previousDistance
                    if (previous != null && previous > 0f) {
                        val zoom = distance / previous
                        val nextFontSize = ((currentFontSize * zoom).coerceIn(MinReaderFontSizeSp, MaxReaderFontSizeSp) * 4f)
                            .roundToInt() / 4f
                        if (abs(nextFontSize - currentFontSize) >= 0.25f) {
                            onFontSizeChange(nextFontSize)
                            pressed.forEach { it.consume() }
                        }
                    }
                    previousDistance = distance
                } else {
                    previousDistance = null
                }
            }
        }
    }
}

private fun pointerDistance(
    first: Offset,
    second: Offset,
): Float {
    val dx = first.x - second.x
    val dy = first.y - second.y
    return sqrt(dx * dx + dy * dy)
}

private fun dynamicReaderLineHeight(
    fontSizeSp: Float,
    preferredMultiplier: Float,
): Float {
    val scale = readerFontScale(fontSizeSp)
    return (preferredMultiplier * (0.88f + 0.12f * scale)).coerceIn(1.08f, 2.2f)
}

private fun dynamicVerseSpacing(fontSizeSp: Float): Dp {
    val scale = readerFontScale(fontSizeSp)
    return (2f + 6f * scale).dp
}

private fun dynamicVersePadding(fontSizeSp: Float): Dp {
    val scale = readerFontScale(fontSizeSp)
    return (8f + 7f * scale).dp
}

private fun dynamicActionButtonSize(fontSizeSp: Float): Dp {
    val scale = readerFontScale(fontSizeSp)
    return (28f + 12f * scale).dp
}

private fun dynamicActionIconSize(fontSizeSp: Float): Dp {
    val scale = readerFontScale(fontSizeSp)
    return (16f + 8f * scale).dp
}

private fun readerFontScale(fontSizeSp: Float): Float {
    return ((fontSizeSp - MinReaderFontSizeSp) / (MaxReaderFontSizeSp - MinReaderFontSizeSp)).coerceIn(0f, 1f)
}

private const val MinReaderFontSizeSp = 12f
private const val MaxReaderFontSizeSp = 28f

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ScaledVerseIconButton(
    fontSizeSp: Float,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(dynamicActionButtonSize(fontSizeSp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
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

    val trackHeightPx = trackSize.height.toFloat()
    val minThumbHeightPx = with(density) { 36.dp.toPx() }
    val canShowThumb = trackHeightPx >= minThumbHeightPx
    val thumbHeightPx = if (canShowThumb) {
        (trackHeightPx * visibleCount / itemCount.toFloat()).coerceIn(minThumbHeightPx, trackHeightPx)
    } else {
        0f
    }
    val maxFirstIndex = (itemCount - visibleCount).coerceAtLeast(1)
    val firstIndex = listState.firstVisibleItemIndex.coerceIn(0, maxFirstIndex)
    val thumbOffsetPx = if (canShowThumb) {
        ((trackHeightPx - thumbHeightPx) * firstIndex / maxFirstIndex).coerceIn(0f, trackHeightPx - thumbHeightPx)
    } else {
        0f
    }

    fun scrollToPosition(y: Float) {
        if (!canShowThumb) return
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
        if (canShowThumb) {
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
