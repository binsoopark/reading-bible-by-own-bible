package com.soobinpark.appcraft.readingbible.feature.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileDiagnostic
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileIssue
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileIssueSeverity

@Composable
fun LibraryRoute(
    dataFolderUri: Uri?,
    localDataRoot: java.io.File?,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(dataFolderUri, localDataRoot) {
        viewModel.setDataFolder(dataFolderUri, localDataRoot)
    }

    LibraryScreen(
        state = uiState,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
private fun LibraryScreen(
    state: LibraryUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentPadding = PaddingValues(bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onRefresh) {
                    Text(stringResource(R.string.action_refresh))
                }
            }
        }
        item {
            SourceCard(state)
        }
        if (state.isLoading) {
            item { CircularProgressIndicator() }
        } else {
            state.diagnostic?.let { diagnostic ->
                item { SummaryGrid(diagnostic) }
                item { VersionSection(diagnostic) }
                item { AudioSection(diagnostic) }
                item { IssueSection(diagnostic) }
            }
        }
    }
}

@Composable
private fun AudioSection(diagnostic: BibleFileDiagnostic) {
    val context = LocalContext.current
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.library_audio), style = MaterialTheme.typography.titleMedium)
            if (diagnostic.audioFiles.isEmpty()) {
                Text(stringResource(R.string.library_no_audio))
            } else {
                diagnostic.audioFiles.take(20).forEach { audio ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(audio.name, modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(audio.uri, "audio/mpeg")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                        ) {
                            Text(stringResource(R.string.action_play))
                        }
                    }
                }
                if (diagnostic.audioFiles.size > 20) {
                    Text(stringResource(R.string.library_more_audio, diagnostic.audioFiles.size - 20))
                }
            }
        }
    }
}

@Composable
private fun SourceCard(state: LibraryUiState) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.library_data_location), style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.dataFolderUri?.toString() ?: state.dataRoot.absolutePath,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (state.dataFolderUri != null) {
                    stringResource(R.string.library_scanning_selected)
                } else {
                    stringResource(R.string.library_scanning_default)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SummaryGrid(diagnostic: BibleFileDiagnostic) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(stringResource(R.string.library_all_files), diagnostic.scannedFileCount.toString(), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.library_versions), diagnostic.versions.size.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(".lfa", diagnostic.lfaCount.toString(), Modifier.weight(1f))
            SummaryCard(".bdf", stringResource(R.string.library_file_count, diagnostic.bdfFileCount), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(stringResource(R.string.library_complete_bdf), diagnostic.completeBdfVersionCount.toString(), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.library_issues), diagnostic.issues.size.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(".lfb", diagnostic.lfbCount.toString(), Modifier.weight(1f))
            SummaryCard("MP3", diagnostic.mp3Count.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VersionSection(diagnostic: BibleFileDiagnostic) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.library_detected_versions), style = MaterialTheme.typography.titleMedium)
            if (diagnostic.versions.isEmpty()) {
                Text(stringResource(R.string.library_no_versions))
            } else {
                diagnostic.versions.forEach { version ->
                    AssistChip(
                        onClick = {},
                        label = { Text("${version.displayName} · ${version.code}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueSection(diagnostic: BibleFileDiagnostic) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.library_diagnostics), style = MaterialTheme.typography.titleMedium)
            if (diagnostic.issues.isEmpty()) {
                Text(stringResource(R.string.library_diagnostics_ok))
            } else {
                diagnostic.issues.forEach { issue ->
                    IssueRow(issue)
                }
            }
        }
    }
}

@Composable
private fun IssueRow(issue: BibleFileIssue) {
    val color = when (issue.severity) {
        BibleFileIssueSeverity.Error -> MaterialTheme.colorScheme.error
        BibleFileIssueSeverity.Warning -> MaterialTheme.colorScheme.tertiary
        BibleFileIssueSeverity.Info -> MaterialTheme.colorScheme.primary
    }
    val label = when (issue.severity) {
        BibleFileIssueSeverity.Error -> stringResource(R.string.severity_error)
        BibleFileIssueSeverity.Warning -> stringResource(R.string.severity_warning)
        BibleFileIssueSeverity.Info -> stringResource(R.string.severity_info)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label · ${issue.title}", color = color, fontWeight = FontWeight.SemiBold)
        Text(issue.detail, style = MaterialTheme.typography.bodyMedium)
    }
}
