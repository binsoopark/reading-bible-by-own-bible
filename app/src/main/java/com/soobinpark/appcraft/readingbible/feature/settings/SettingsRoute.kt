package com.soobinpark.appcraft.readingbible.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import com.soobinpark.appcraft.readingbible.feature.library.LibraryRoute
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(
    dataFolderUri: Uri?,
    readingStyle: ReadingStyle,
    onDataFolderSelected: (Uri?) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onLineHeightChanged: (Float) -> Unit,
    onPaletteSelected: (ReadingPalette) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onMultitouchZoomEnabledChanged: (Boolean) -> Unit,
    onBoldTextEnabledChanged: (Boolean) -> Unit,
    onShowNotesInReaderChanged: (Boolean) -> Unit,
    onExportRecords: () -> String,
    onImportRecords: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var importExportMessage by remember { mutableStateOf<String?>(null) }
    var showBibleDiagnostics by remember { mutableStateOf(false) }
    val appVersion = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "알 수 없음"
        }.getOrDefault("알 수 없음")
    }
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            onDataFolderSelected(uri)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(onExportRecords().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                importExportMessage = "구절 기록을 내보냈습니다."
            }.onFailure {
                importExportMessage = "내보내기에 실패했습니다."
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }.onSuccess { json ->
                onImportRecords(json)
                importExportMessage = "구절 기록을 가져왔습니다."
            }.onFailure {
                importExportMessage = "가져오기에 실패했습니다."
            }
        }
    }

    if (showBibleDiagnostics) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 12.dp),
        ) {
            Button(
                onClick = { showBibleDiagnostics = false },
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Text("설정으로 돌아가기")
            }
            LibraryRoute(
                dataFolderUri = dataFolderUri,
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("설정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("성경 데이터 폴더", style = MaterialTheme.typography.titleMedium)
                    Text(dataFolderUri?.toString() ?: "아직 선택한 폴더가 없습니다. 선택하지 않으면 기본 /sdcard/bible 폴더를 시도합니다.")
                    Button(onClick = { folderPicker.launch(null) }) {
                        Text("bdf/lfa 폴더 선택")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("처음 시작", style = MaterialTheme.typography.titleMedium)
                    Text("1. Lifove Bible 호환 bdf/lfa 파일이 들어 있는 폴더를 선택합니다.")
                    Text("2. 설정의 성경 파일 확인에서 감지된 파일과 누락 파일을 확인합니다.")
                    Text("3. 읽기 탭에서 역본, 책, 장을 선택해 읽습니다.")
                    Text("이 앱은 성경 본문 데이터를 포함하지 않으며, 사용자가 가진 파일을 읽습니다.")
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("성경 파일 확인", style = MaterialTheme.typography.titleMedium)
                    Text("선택한 bdf/lfa 폴더의 역본, 오디오 파일, 누락 파일과 진단 결과를 확인합니다.")
                    Button(onClick = { showBibleDiagnostics = true }) {
                        Text("성경 파일 확인하기")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("기록 가져오기 / 내보내기", style = MaterialTheme.typography.titleMedium)
                    Text("북마크, 메모, 하이라이트, 읽음 체크를 JSON 파일로 백업하거나 복원합니다.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("reading-bible-records.json") }) {
                            Text("내보내기")
                        }
                        Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }) {
                            Text("가져오기")
                        }
                    }
                    importExportMessage?.let { Text(it) }
                }
            }
        }
        item {
            ReadingStyleCard(
                readingStyle = readingStyle,
                onFontSizeChanged = onFontSizeChanged,
                onLineHeightChanged = onLineHeightChanged,
                onPaletteSelected = onPaletteSelected,
                onKeepScreenOnChanged = onKeepScreenOnChanged,
                onMultitouchZoomEnabledChanged = onMultitouchZoomEnabledChanged,
                onBoldTextEnabledChanged = onBoldTextEnabledChanged,
                onShowNotesInReaderChanged = onShowNotesInReaderChanged,
            )
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("앱 정보", style = MaterialTheme.typography.titleMedium)
                    Text("버전 $appVersion")
                    Text(stringResource(R.string.app_info_lifove_compat))
                }
            }
        }
    }
}

@Composable
private fun ReadingStyleCard(
    readingStyle: ReadingStyle,
    onFontSizeChanged: (Float) -> Unit,
    onLineHeightChanged: (Float) -> Unit,
    onPaletteSelected: (ReadingPalette) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onMultitouchZoomEnabledChanged: (Boolean) -> Unit,
    onBoldTextEnabledChanged: (Boolean) -> Unit,
    onShowNotesInReaderChanged: (Boolean) -> Unit,
) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("읽기 스타일", style = MaterialTheme.typography.titleMedium)
            ToggleSettingRow(
                title = "화면 꺼지지 않도록 유지",
                description = "본문을 읽는 동안 화면 자동 꺼짐을 막습니다.",
                checked = readingStyle.keepScreenOn,
                onCheckedChange = onKeepScreenOnChanged,
            )
            ToggleSettingRow(
                title = "멀티터치 줌 사용",
                description = "본문에서 두 손가락으로 글자 크기를 조절합니다.",
                checked = readingStyle.multitouchZoomEnabled,
                onCheckedChange = onMultitouchZoomEnabledChanged,
            )
            ToggleSettingRow(
                title = "볼드체 적용",
                description = "본문 성경 구절을 굵게 표시합니다.",
                checked = readingStyle.boldTextEnabled,
                onCheckedChange = onBoldTextEnabledChanged,
            )
            ToggleSettingRow(
                title = "본문 메모 아이콘 표시",
                description = "메모가 작성된 절 옆에 표시를 보여줍니다.",
                checked = readingStyle.showNotesInReader,
                onCheckedChange = onShowNotesInReaderChanged,
            )
            SettingSlider(
                title = "글자 크기",
                valueLabel = "${readingStyle.fontSizeSp.roundToInt()}sp",
                value = readingStyle.fontSizeSp,
                valueRange = 12f..28f,
                steps = 15,
                onValueChange = onFontSizeChanged,
            )
            SettingSlider(
                title = "본문 줄 간격",
                valueLabel = "${(readingStyle.lineHeightMultiplier * 100).roundToInt()}%",
                value = readingStyle.lineHeightMultiplier,
                valueRange = 1.2f..2.2f,
                steps = 9,
                onValueChange = onLineHeightChanged,
            )
            Text("팔레트", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingPalette.entries.take(3).forEach { palette ->
                    PaletteChip(
                        palette = palette,
                        selected = readingStyle.palette == palette,
                        onPaletteSelected = onPaletteSelected,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingPalette.entries.drop(3).forEach { palette ->
                    PaletteChip(
                        palette = palette,
                        selected = readingStyle.palette == palette,
                        onPaletteSelected = onPaletteSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingSlider(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Composable
private fun PaletteChip(
    palette: ReadingPalette,
    selected: Boolean,
    onPaletteSelected: (ReadingPalette) -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = { onPaletteSelected(palette) },
        label = { Text(palette.label) },
    )
}
