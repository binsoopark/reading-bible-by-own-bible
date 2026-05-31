package com.soobinpark.appcraft.readingbible.feature.search

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SearchRoute(
    dataFolderUri: Uri?,
    localDataRoot: File?,
    onResultLongPressed: (BibleSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(dataFolderUri, localDataRoot) {
        viewModel.setDataFolder(dataFolderUri, localDataRoot)
    }

    SearchScreen(
        state = uiState,
        onQueryChanged = viewModel::updateQuery,
        onSearch = viewModel::search,
        onVersionSelected = viewModel::selectVersion,
        onShowNextPage = viewModel::showNextPage,
        onResultLongPressed = onResultLongPressed,
        modifier = modifier,
    )
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onVersionSelected: (BibleVersion) -> Unit,
    onShowNextPage: () -> Unit,
    onResultLongPressed: (BibleSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showVersionSheet by androidx.compose.runtime.remember { mutableStateOf(false) }
    val resultListState = rememberLazyListState()
    val resultStartIndex = 2 +
        if (state.isLoading || state.isSearching) 1 else 0 +
        if (state.message != null) 1 else 0 +
        if (state.results.isNotEmpty()) 1 else 0
    val currentResult by androidx.compose.runtime.remember(state.visibleResults, resultStartIndex) {
        derivedStateOf {
            state.visibleResults.getOrNull((resultListState.firstVisibleItemIndex - resultStartIndex).coerceAtLeast(0))
        }
    }
    val totalListItems = resultStartIndex +
        state.visibleResults.size +
        if (state.visibleResults.size < state.results.size) 1 else 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        LazyColumn(
            state = resultListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(end = 30.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("검색", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                SearchInputCard(
                    state = state,
                    onQueryChanged = onQueryChanged,
                    onSearch = onSearch,
                    onVersionClick = { showVersionSheet = true },
                )
            }
            if (state.isLoading || state.isSearching) {
                item { SearchProgress(state) }
            }
            state.message?.let { message ->
                item {
                    Card {
                        Text(
                            text = message,
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (state.results.isNotEmpty()) {
                item {
                    Text(
                        text = "전체 ${state.results.size}개 중 ${state.visibleResults.size}개 표시",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.visibleResults, key = { "${it.verse.versionCode}-${it.verse.bookIndex}-${it.verse.chapter}-${it.verse.verse}" }) { result ->
                SearchResultCard(
                    result = result,
                    query = state.query,
                    onLongPress = { onResultLongPressed(result) },
                )
            }
            if (state.visibleResults.size < state.results.size) {
                item {
                    Button(onClick = onShowNextPage, modifier = Modifier.fillMaxWidth()) {
                        Text("다음 100개 보기")
                    }
                }
            }
        }
        if (state.results.isNotEmpty()) {
            Card(modifier = Modifier.align(Alignment.TopCenter)) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentResult?.let { "${it.book.koreanName} ${it.verse.chapter}장" }.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        SearchScrollBar(
            listState = resultListState,
            itemCount = totalListItems,
            labelForIndex = { index ->
                state.visibleResults.getOrNull(index - resultStartIndex)
                    ?.let { "${it.book.koreanName} ${it.verse.chapter}장" }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }

    if (showVersionSheet) {
        SearchVersionPickerDialog(
            versions = state.versions,
            selectedVersion = state.selectedVersion,
            onDismiss = { showVersionSheet = false },
            onVersionSelected = {
                onVersionSelected(it)
                showVersionSheet = false
            },
        )
    }
}

@Composable
private fun SearchInputCard(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onVersionClick: () -> Unit,
) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("본문 검색", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                SuggestionChip(
                    onClick = onVersionClick,
                    label = { Text(state.selectedVersion?.displayName ?: "역본 선택") },
                )
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("검색어") },
                placeholder = { Text("두 글자 이상 입력") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSearch,
                    enabled = !state.isLoading && !state.isSearching,
                ) {
                    Text("검색")
                }
                Text(
                    text = if (state.results.isEmpty()) "전체 결과를 검색합니다" else "총 ${state.results.size}개 결과",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchProgress(state: SearchUiState) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Text(
                    text = state.searchProgressMessage.ifBlank { "검색하는 중입니다." },
                    modifier = Modifier.weight(1f),
                )
            }
            state.searchProgress?.let { progress ->
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun VersionSelectorList(
    versions: List<BibleVersion>,
    selectedVersion: BibleVersion?,
    onVersionSelected: (BibleVersion) -> Unit,
) {
    val groupedVersions = versions
        .sortedWith(compareBy({ it.displayName.lowercase() }, { it.code.lowercase() }))
        .groupBy { it.languageGroup() }
        .toSortedMap(compareBy { VersionLanguageGroup.orderOf(it) })
    val listState = rememberLazyListState()
    val itemCount = groupedVersions.values.sumOf { it.size } + groupedVersions.size
    Box(modifier = Modifier.heightIn(max = 190.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(end = 28.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            groupedVersions.forEach { (group, groupVersions) ->
                item(key = "header-$group") {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(groupVersions, key = { "${it.sourceType}-${it.code}" }) { version ->
                    FilterChip(
                        selected = selectedVersion == version,
                        onClick = { onVersionSelected(version) },
                        label = { Text("${version.displayName} · ${version.code}") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        SearchScrollBar(
            listState = listState,
            itemCount = itemCount,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun SearchVersionPickerDialog(
    versions: List<BibleVersion>,
    selectedVersion: BibleVersion?,
    onDismiss: () -> Unit,
    onVersionSelected: (BibleVersion) -> Unit,
) {
    AnimatedPickerDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("검색 역본 선택", style = MaterialTheme.typography.titleLarge)
            VersionSelectorList(
                versions = versions,
                selectedVersion = selectedVersion,
                onVersionSelected = onVersionSelected,
            )
        }
    }
}

@Composable
private fun AnimatedPickerDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(120)) +
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                ),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SearchResultCard(
    result: BibleSearchResult,
    query: String,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongPress,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${result.book.koreanName} ${result.verse.chapter}:${result.verse.verse}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(highlightQuery(result.snippet, query), style = MaterialTheme.typography.bodyLarge)
            Text(result.verse.versionCode, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun highlightQuery(
    text: String,
    query: String,
) = buildAnnotatedString {
    val needle = query.trim()
    if (needle.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    var start = 0
    while (start < text.length) {
        val match = text.indexOf(needle, startIndex = start, ignoreCase = true)
        if (match < 0) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, match))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
            append(text.substring(match, match + needle.length))
        }
        start = match + needle.length
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

@Composable
private fun SearchScrollBar(
    listState: LazyListState,
    itemCount: Int,
    labelForIndex: ((Int) -> String?)? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var trackSize by androidx.compose.runtime.remember { mutableStateOf(IntSize.Zero) }
    var isDragging by androidx.compose.runtime.remember { mutableStateOf(false) }
    var previewFirstIndex by androidx.compose.runtime.remember { mutableStateOf<Int?>(null) }
    val visibleCount by androidx.compose.runtime.remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) }
    }
    if (itemCount <= visibleCount) return
    val trackHeightPx = trackSize.height.toFloat()
    val minThumbHeightPx = with(density) { 28.dp.toPx() }
    val thumbHeightPx = if (trackHeightPx >= minThumbHeightPx) {
        (trackHeightPx * visibleCount / itemCount.toFloat()).coerceIn(minThumbHeightPx, trackHeightPx)
    } else {
        0f
    }
    val maxFirstIndex = (itemCount - visibleCount).coerceAtLeast(1)
    val firstIndex = listState.firstVisibleItemIndex.coerceIn(0, maxFirstIndex)
    val labelIndex = (previewFirstIndex ?: firstIndex).coerceIn(0, itemCount - 1)
    val dragLabel = if (isDragging) labelForIndex?.invoke(labelIndex) else null
    val thumbOffsetPx = ((trackHeightPx - thumbHeightPx) * firstIndex / maxFirstIndex)
        .coerceIn(0f, (trackHeightPx - thumbHeightPx).coerceAtLeast(0f))

    fun targetIndexForPosition(y: Float): Int {
        val movable = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
        val ratio = ((y - thumbHeightPx / 2f) / movable).coerceIn(0f, 1f)
        return (ratio * maxFirstIndex).roundToInt().coerceIn(0, itemCount - 1)
    }

    fun scrollToPosition(y: Float) {
        val targetIndex = targetIndexForPosition(y)
        previewFirstIndex = targetIndex
        coroutineScope.launch {
            listState.scrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .width(if (labelForIndex == null) 24.dp else 154.dp)
            .fillMaxHeight()
            .onSizeChanged { trackSize = it }
            .pointerInput(itemCount, trackSize) {
                detectVerticalDragGestures(
                    onDragStart = { offset: Offset ->
                        isDragging = true
                        scrollToPosition(offset.y)
                    },
                    onVerticalDrag = { change, _ -> scrollToPosition(change.position.y) },
                    onDragEnd = {
                        isDragging = false
                        previewFirstIndex = null
                    },
                    onDragCancel = {
                        isDragging = false
                        previewFirstIndex = null
                    },
                )
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        dragLabel?.let { label ->
            Box(
                modifier = Modifier
                    .offset(y = with(density) { thumbOffsetPx.toDp() })
                    .padding(end = 18.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopEnd),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), RoundedCornerShape(99.dp)),
        )
        Box(
            modifier = Modifier
                .padding(top = with(density) { thumbOffsetPx.toDp() })
                .width(10.dp)
                .height(with(density) { thumbHeightPx.toDp() })
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(99.dp)),
        )
    }
}
