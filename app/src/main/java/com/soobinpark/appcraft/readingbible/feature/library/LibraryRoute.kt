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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileDiagnostic
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileIssue
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileIssueSeverity

@Composable
fun LibraryRoute(
    dataFolderUri: Uri?,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(dataFolderUri) {
        viewModel.setDataFolderUri(dataFolderUri)
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
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "성경 파일",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onRefresh) {
                    Text("새로고침")
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
            Text("오디오 파일", style = MaterialTheme.typography.titleMedium)
            if (diagnostic.audioFiles.isEmpty()) {
                Text("감지된 MP3 파일이 없습니다.")
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
                            Text("재생")
                        }
                    }
                }
                if (diagnostic.audioFiles.size > 20) {
                    Text("그 외 ${diagnostic.audioFiles.size - 20}개 파일이 더 있습니다.")
                }
            }
        }
    }
}

@Composable
private fun SourceCard(state: LibraryUiState) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("데이터 위치", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.dataFolderUri?.toString() ?: state.dataRoot.absolutePath,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (state.dataFolderUri != null) {
                    "설정 탭에서 선택한 폴더를 검사 중입니다."
                } else {
                    "선택한 폴더가 없어 기본 /sdcard/bible 폴더를 검사합니다."
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
            SummaryCard("전체 파일", diagnostic.scannedFileCount.toString(), Modifier.weight(1f))
            SummaryCard("역본", diagnostic.versions.size.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(".lfa", diagnostic.lfaCount.toString(), Modifier.weight(1f))
            SummaryCard(".bdf", "${diagnostic.bdfFileCount}개", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("완성 bdf", diagnostic.completeBdfVersionCount.toString(), Modifier.weight(1f))
            SummaryCard("이슈", diagnostic.issues.size.toString(), Modifier.weight(1f))
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
            Text("감지된 역본", style = MaterialTheme.typography.titleMedium)
            if (diagnostic.versions.isEmpty()) {
                Text("읽을 수 있는 역본이 아직 없습니다.")
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
            Text("진단 결과", style = MaterialTheme.typography.titleMedium)
            if (diagnostic.issues.isEmpty()) {
                Text("큰 문제 없이 읽을 수 있는 데이터 폴더로 보입니다.")
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
        BibleFileIssueSeverity.Error -> "오류"
        BibleFileIssueSeverity.Warning -> "주의"
        BibleFileIssueSeverity.Info -> "정보"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label · ${issue.title}", color = color, fontWeight = FontWeight.SemiBold)
        Text(issue.detail, style = MaterialTheme.typography.bodyMedium)
    }
}
