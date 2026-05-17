package com.soobinpark.appcraft.readingbible.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(
    dataFolderUri: Uri?,
    readingStyle: ReadingStyle,
    onDataFolderSelected: (Uri?) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onLineHeightChanged: (Float) -> Unit,
    onPaletteSelected: (ReadingPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
            ReadingStyleCard(
                readingStyle = readingStyle,
                onFontSizeChanged = onFontSizeChanged,
                onLineHeightChanged = onLineHeightChanged,
                onPaletteSelected = onPaletteSelected,
            )
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("앱 정보", style = MaterialTheme.typography.titleMedium)
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
) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("읽기 스타일", style = MaterialTheme.typography.titleMedium)
            SettingSlider(
                title = "글자 크기",
                valueLabel = "${readingStyle.fontSizeSp.roundToInt()}sp",
                value = readingStyle.fontSizeSp,
                valueRange = 14f..28f,
                steps = 13,
                onValueChange = onFontSizeChanged,
            )
            SettingSlider(
                title = "줄 간격",
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
