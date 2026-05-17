package com.soobinpark.appcraft.readingbible.feature.reader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog

@Composable
fun ReaderRoute(
    dataFolderUri: Uri?,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(dataFolderUri) {
        viewModel.setDataFolderUri(dataFolderUri)
    }
    ReaderScreen(
        state = uiState,
        onVersionSelected = viewModel::selectVersion,
        onBookChapterSelected = viewModel::selectBookAndChapter,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    state: ReaderUiState,
    onVersionSelected: (com.soobinpark.appcraft.readingbible.domain.model.BibleVersion) -> Unit,
    onBookChapterSelected: (Int, Int) -> Unit,
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
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.verses, key = { "${it.versionCode}-${it.bookIndex}-${it.chapter}-${it.verse}" }) { verse ->
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "${verse.verse}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = verse.text,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVersionSheet) {
        ModalBottomSheet(onDismissRequest = { showVersionSheet = false }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("역본 선택", style = MaterialTheme.typography.titleLarge)
                state.versions.forEach { version ->
                    FilterChip(
                        selected = state.selectedVersion == version,
                        onClick = {
                            onVersionSelected(version)
                            showVersionSheet = false
                        },
                        label = { Text("${version.code} · ${version.sourceType}") },
                    )
                }
            }
        }
    }

    if (showBookSheet) {
        ModalBottomSheet(onDismissRequest = { showBookSheet = false }) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(BibleCatalog.books) { book ->
                    TextButton(
                        onClick = {
                            onBookChapterSelected(book.index, 1)
                            showBookSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${book.koreanName} · ${book.englishName} · ${book.chapterCount}장")
                    }
                }
            }
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
