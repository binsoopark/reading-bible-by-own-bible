package com.soobinpark.appcraft.readingbible.feature.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import java.text.DateFormat
import java.util.Date

@Composable
fun RecordsRoute(
    bookmarks: List<VerseBookmark>,
    onNoteChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("기록", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("북마크", style = MaterialTheme.typography.titleMedium)
                    Text(if (bookmarks.isEmpty()) "아직 저장한 구절이 없습니다." else "${bookmarks.size}개 구절을 저장했습니다.")
                }
            }
        }
        items(bookmarks, key = { it.key }) { bookmark ->
            BookmarkCard(
                bookmark = bookmark,
                onNoteChanged = onNoteChanged,
            )
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmark: VerseBookmark,
    onNoteChanged: (String, String) -> Unit,
) {
    val book = BibleCatalog.books.getOrNull(bookmark.bookIndex)
    val reference = listOfNotNull(
        book?.koreanName,
        "${bookmark.chapter}:${bookmark.verse}",
    ).joinToString(" ")
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = reference,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(bookmark.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${bookmark.versionCode} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(bookmark.createdAtMillis))}",
                style = MaterialTheme.typography.labelMedium,
            )
            if (bookmark.highlight != VerseHighlight.None) {
                AssistChip(
                    onClick = {},
                    label = { Text("${bookmark.highlight.label} 하이라이트") },
                )
            }
            OutlinedTextField(
                value = bookmark.note,
                onValueChange = { onNoteChanged(bookmark.key, it) },
                modifier = Modifier.padding(top = 6.dp),
                label = { Text("메모") },
                placeholder = { Text("이 구절에 대한 생각을 적어두기") },
                minLines = 2,
            )
        }
    }
}
