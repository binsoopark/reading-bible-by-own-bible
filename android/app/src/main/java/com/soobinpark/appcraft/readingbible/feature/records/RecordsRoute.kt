package com.soobinpark.appcraft.readingbible.feature.records

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.RecordFilter
import com.soobinpark.appcraft.readingbible.domain.model.RecordShareFormatter
import com.soobinpark.appcraft.readingbible.domain.model.RecordSortOrder
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordsRoute(
    bookmarks: List<VerseBookmark>,
    onNoteChanged: (String, String) -> Unit,
    onOpenBookmark: (VerseBookmark) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(RecordFilter.Bookmark) }
    var query by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(RecordSortOrder.Recent) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    val context = LocalContext.current
    val filtered = remember(bookmarks, filter, query, sortOrder) {
        val normalizedQuery = query.trim().lowercase()
        bookmarks
            .asSequence()
            .filter { filter.matches(it) }
            .filter { bookmark ->
                if (normalizedQuery.isEmpty()) return@filter true
                val book = BibleCatalog.books.getOrNull(bookmark.bookIndex)
                listOf(
                    bookmark.text,
                    bookmark.note,
                    bookmark.versionCode,
                    book?.koreanName.orEmpty(),
                    book?.englishName.orEmpty(),
                    "${book?.koreanName.orEmpty()} ${bookmark.chapter}:${bookmark.verse}",
                    "${bookmark.chapter}:${bookmark.verse}",
                ).any { it.lowercase().contains(normalizedQuery) }
            }
            .let { records ->
                when (sortOrder) {
                    RecordSortOrder.Recent -> records.sortedByDescending { it.createdAtMillis }
                    RecordSortOrder.Bible -> records.sortedWith(
                        compareBy({ it.bookIndex }, { it.chapter }, { it.verse }, { it.versionCode.lowercase() }),
                    )
                }
            }
            .toList()
    }
    val selected = remember(filtered, selectedKeys) { filtered.filter { selectedKeys.contains(it.key) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("메모", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RecordFilter.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = filter == item,
                    onClick = {
                        filter = item
                        selectedKeys = emptySet()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = RecordFilter.entries.size),
                ) {
                    Text(item.label)
                }
            }
        }
        Text(
            text = filterDescription(filter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                selectedKeys = emptySet()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("기록 검색") },
            placeholder = { Text("본문, 메모, 역본, 성경 위치") },
            singleLine = true,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RecordSortOrder.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = sortOrder == item,
                    onClick = { sortOrder = item },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = RecordSortOrder.entries.size),
                ) {
                    Text(item.label)
                }
            }
        }
        if (selectedKeys.isNotEmpty()) {
            SelectedRecordsBar(
                count = selectedKeys.size,
                onClear = { selectedKeys = emptySet() },
                onCopy = {
                    val text = RecordShareFormatter.format(selected)
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("성경 구절", text))
                    Toast.makeText(context, "선택한 구절을 복사했습니다", Toast.LENGTH_SHORT).show()
                    selectedKeys = emptySet()
                },
                onShare = {
                    val text = RecordShareFormatter.format(selected)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "구절 공유"))
                    selectedKeys = emptySet()
                },
            )
        }
        if (filtered.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (query.isBlank()) emptyTitle(filter) else "검색 결과 없음", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (query.isBlank()) emptyMessage(filter) else "다른 검색어로 기록을 찾아보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.key }) { bookmark ->
                    val isSelected = selectedKeys.contains(bookmark.key)
                    BookmarkCard(
                        bookmark = bookmark,
                        filter = filter,
                        isSelected = isSelected,
                        onNoteChanged = onNoteChanged,
                        onClick = {
                            if (selectedKeys.isNotEmpty()) {
                                selectedKeys = selectedKeys.toggle(bookmark.key)
                            } else {
                                onOpenBookmark(bookmark)
                            }
                        },
                        onLongClick = {
                            selectedKeys = selectedKeys.toggle(bookmark.key)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkCard(
    bookmark: VerseBookmark,
    filter: RecordFilter,
    isSelected: Boolean,
    onNoteChanged: (String, String) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val book = BibleCatalog.books.getOrNull(bookmark.bookIndex)
    val reference = listOfNotNull(book?.koreanName, "${bookmark.chapter}:${bookmark.verse}").joinToString(" ")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (bookmark.highlight != VerseHighlight.None) {
                    HighlightSwatch(highlight = bookmark.highlight)
                }
            }
            Text(bookmark.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${bookmark.versionCode} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(bookmark.createdAtMillis))}",
                style = MaterialTheme.typography.labelMedium,
            )
            if (filter == RecordFilter.Note || bookmark.note.isNotBlank()) {
                OutlinedTextField(
                    value = bookmark.note,
                    onValueChange = { onNoteChanged(bookmark.key, it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    label = { Text("메모") },
                    placeholder = { Text("이 구절에 대한 생각을 적어두기") },
                    minLines = 2,
                )
            }
        }
    }
}

@Composable
private fun HighlightSwatch(highlight: VerseHighlight) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(highlightSwatchColor(highlight), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape),
    )
}

@Composable
private fun SelectedRecordsBar(
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

private fun Set<String>.toggle(key: String): Set<String> {
    return if (contains(key)) this - key else this + key
}

private fun filterDescription(filter: RecordFilter): String {
    return when (filter) {
        RecordFilter.Bookmark -> "북마크한 구절만 표시합니다."
        RecordFilter.Highlight -> "형광펜으로 표시한 구절만 표시합니다."
        RecordFilter.Read -> "읽음으로 체크한 구절만 표시합니다."
        RecordFilter.Note -> "메모가 있는 구절만 표시합니다."
    }
}

private fun emptyTitle(filter: RecordFilter): String {
    return when (filter) {
        RecordFilter.Bookmark -> "북마크 없음"
        RecordFilter.Highlight -> "형광펜 표시 없음"
        RecordFilter.Read -> "읽음 표시 없음"
        RecordFilter.Note -> "메모 없음"
    }
}

private fun emptyMessage(filter: RecordFilter): String {
    return when (filter) {
        RecordFilter.Bookmark -> "읽기 탭에서 북마크 아이콘을 누르면 여기에 표시됩니다."
        RecordFilter.Highlight -> "읽기 탭에서 형광펜 아이콘을 눌러 표시하면 여기에 나타납니다."
        RecordFilter.Read -> "읽기 탭에서 읽음 체크를 하면 여기에 표시됩니다."
        RecordFilter.Note -> "기록 카드에서 메모를 작성하면 여기에 표시됩니다."
    }
}

private fun highlightSwatchColor(highlight: VerseHighlight): Color {
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
