package com.soobinpark.appcraft.readingbible.feature.search

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion

@Composable
fun SearchRoute(
    dataFolderUri: Uri?,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(dataFolderUri) {
        viewModel.setDataFolderUri(dataFolderUri)
    }

    SearchScreen(
        state = uiState,
        onQueryChanged = viewModel::updateQuery,
        onSearch = viewModel::search,
        onVersionSelected = viewModel::selectVersion,
        modifier = modifier,
    )
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onVersionSelected: (BibleVersion) -> Unit,
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
            Text("검색", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            SearchInputCard(
                state = state,
                onQueryChanged = onQueryChanged,
                onSearch = onSearch,
                onVersionSelected = onVersionSelected,
            )
        }
        if (state.isLoading || state.isSearching) {
            item { CircularProgressIndicator() }
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
        items(state.results, key = { "${it.verse.versionCode}-${it.verse.bookIndex}-${it.verse.chapter}-${it.verse.verse}" }) { result ->
            SearchResultCard(result)
        }
    }
}

@Composable
private fun SearchInputCard(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onVersionSelected: (BibleVersion) -> Unit,
) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("본문 검색", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("검색어") },
                placeholder = { Text("두 글자 이상 입력") },
            )
            if (state.versions.isNotEmpty()) {
                Text("역본", style = MaterialTheme.typography.labelLarge)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.versions, key = { "${it.sourceType}-${it.code}" }) { version ->
                        FilterChip(
                            selected = state.selectedVersion == version,
                            onClick = { onVersionSelected(version) },
                            label = { Text("${version.displayName} · ${version.code}") },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSearch,
                    enabled = !state.isLoading && !state.isSearching,
                ) {
                    Text("검색")
                }
                Text(
                    text = if (state.results.isEmpty()) "최대 100개 결과" else "${state.results.size}개 결과",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: BibleSearchResult) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${result.book.koreanName} ${result.verse.chapter}:${result.verse.verse}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(result.snippet, style = MaterialTheme.typography.bodyLarge)
            Text(result.verse.versionCode, style = MaterialTheme.typography.labelMedium)
        }
    }
}
