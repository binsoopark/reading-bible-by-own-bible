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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.app.labelRes
import com.soobinpark.appcraft.readingbible.domain.model.localizedName
import java.util.Locale
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
        Text(stringResource(R.string.tab_records), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
                    Text(stringResource(item.labelRes()))
                }
            }
        }
        Text(
            text = stringResource(filter.descriptionRes()),
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
            label = { Text(stringResource(R.string.records_search_label)) },
            placeholder = { Text(stringResource(R.string.records_search_placeholder)) },
            singleLine = true,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RecordSortOrder.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = sortOrder == item,
                    onClick = { sortOrder = item },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = RecordSortOrder.entries.size),
                ) {
                    Text(stringResource(item.labelRes()))
                }
            }
        }
        if (selectedKeys.isNotEmpty()) {
            SelectedRecordsBar(
                count = selectedKeys.size,
                onClear = { selectedKeys = emptySet() },
                onCopy = {
                    val language = context.resources.configuration.locales[0].language
                    val text = RecordShareFormatter.format(selected, { it.localizedName(language) }) { verse, body ->
                        context.getString(R.string.format_verse_line, verse, body)
                    }
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.clipboard_bible_verse), text))
                    Toast.makeText(context, context.getString(R.string.records_copied), Toast.LENGTH_SHORT).show()
                    selectedKeys = emptySet()
                },
                onShare = {
                    val language = context.resources.configuration.locales[0].language
                    val text = RecordShareFormatter.format(selected, { it.localizedName(language) }) { verse, body ->
                        context.getString(R.string.format_verse_line, verse, body)
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_bible_verse)))
                    selectedKeys = emptySet()
                },
            )
        }
        if (filtered.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (query.isBlank()) stringResource(filter.emptyTitleRes()) else stringResource(R.string.records_no_search_results), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (query.isBlank()) stringResource(filter.emptyMessageRes()) else stringResource(R.string.records_try_another_query),
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
    val language = LocalContext.current.resources.configuration.locales[0].language
    val reference = listOfNotNull(book?.localizedName(language), "${bookmark.chapter}:${bookmark.verse}").joinToString(" ")
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
                    label = { Text(stringResource(R.string.record_filter_note)) },
                    placeholder = { Text(stringResource(R.string.records_note_placeholder)) },
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
                text = stringResource(R.string.format_selected_count, count),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text(stringResource(R.string.action_clear_selection)) }
            Button(onClick = onCopy) { Text(stringResource(R.string.action_copy)) }
            Button(onClick = onShare) { Text(stringResource(R.string.action_share)) }
        }
    }
}

private fun Set<String>.toggle(key: String): Set<String> {
    return if (contains(key)) this - key else this + key
}

private fun RecordFilter.descriptionRes(): Int {
    return when (this) {
        RecordFilter.Bookmark -> R.string.records_description_bookmark
        RecordFilter.Highlight -> R.string.records_description_highlight
        RecordFilter.Read -> R.string.records_description_read
        RecordFilter.Note -> R.string.records_description_note
    }
}

private fun RecordFilter.emptyTitleRes(): Int {
    return when (this) {
        RecordFilter.Bookmark -> R.string.records_empty_bookmark_title
        RecordFilter.Highlight -> R.string.records_empty_highlight_title
        RecordFilter.Read -> R.string.records_empty_read_title
        RecordFilter.Note -> R.string.records_empty_note_title
    }
}

private fun RecordFilter.emptyMessageRes(): Int {
    return when (this) {
        RecordFilter.Bookmark -> R.string.records_empty_bookmark_message
        RecordFilter.Highlight -> R.string.records_empty_highlight_message
        RecordFilter.Read -> R.string.records_empty_read_message
        RecordFilter.Note -> R.string.records_empty_note_message
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
