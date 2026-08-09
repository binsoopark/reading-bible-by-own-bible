package com.soobinpark.appcraft.readingbible.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.soobinpark.appcraft.readingbible.app.labelRes
import com.soobinpark.appcraft.readingbible.app.BibleDataDownloadUiState
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.ReadingStyle
import com.soobinpark.appcraft.readingbible.feature.library.LibraryRoute
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(
    dataFolderUri: Uri?,
    localDataRoot: File?,
    dataDownloadState: BibleDataDownloadUiState,
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
    onDownloadBibleData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var importExportMessage by remember { mutableStateOf<String?>(null) }
    var showBibleDiagnostics by remember { mutableStateOf(false) }
    var showHelpGuide by remember { mutableStateOf(false) }
    val appVersion = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: context.getString(R.string.unknown)
        }.getOrDefault(context.getString(R.string.unknown))
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
                importExportMessage = context.getString(R.string.settings_export_success)
            }.onFailure {
                importExportMessage = context.getString(R.string.settings_export_failure)
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
                importExportMessage = context.getString(R.string.settings_import_success)
            }.onFailure {
                importExportMessage = context.getString(R.string.settings_import_failure)
            }
        }
    }

    if (showHelpGuide) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 12.dp),
        ) {
            Button(
                onClick = { showHelpGuide = false },
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_back))
            }
            HelpGuideScreen(modifier = Modifier.weight(1f))
        }
        return
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
                Text(stringResource(R.string.settings_back))
            }
            LibraryRoute(
                dataFolderUri = dataFolderUri,
                localDataRoot = localDataRoot,
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_help_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_help_description))
                    Button(onClick = { showHelpGuide = true }) {
                        Text(stringResource(R.string.settings_help_action))
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_data_folder), style = MaterialTheme.typography.titleMedium)
                    Text(
                        dataFolderUri?.toString()
                            ?: localDataRoot?.absolutePath
                            ?: stringResource(R.string.settings_no_folder),
                    )
                    Button(onClick = { folderPicker.launch(null) }) {
                        Text(stringResource(R.string.settings_choose_folder))
                    }
                }
            }
        }
        item {
            BibleDataDownloadCard(
                state = dataDownloadState,
                onDownloadBibleData = onDownloadBibleData,
            )
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_getting_started), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_step_one))
                    Text(stringResource(R.string.settings_step_two))
                    Text(stringResource(R.string.settings_step_three))
                    Text(stringResource(R.string.settings_data_notice))
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_check_files), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_check_files_description))
                    Button(onClick = { showBibleDiagnostics = true }) {
                        Text(stringResource(R.string.settings_check_files_action))
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_records_transfer), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_records_transfer_description))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("reading-bible-records.json") }) {
                            Text(stringResource(R.string.settings_export))
                        }
                        Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }) {
                            Text(stringResource(R.string.settings_import))
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
                    Text(stringResource(R.string.settings_app_info), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.format_version, appVersion))
                    Text(stringResource(R.string.app_info_lifove_compat))
                }
            }
        }
    }
}

@Composable
private fun BibleDataDownloadCard(
    state: BibleDataDownloadUiState,
    onDownloadBibleData: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_download_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                    )
                }
            }
            Text(
                stringResource(R.string.settings_download_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_download_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.installedRoot?.let {
                        Text(stringResource(R.string.settings_installed_folder, it.absolutePath), style = MaterialTheme.typography.bodySmall)
                    }
                    state.message.takeIf { it.isNotBlank() }?.let { Text(it) }
                    state.progress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = onDownloadBibleData,
                        enabled = !state.isActive,
                    ) {
                        Text(stringResource(R.string.settings_download_action))
                    }
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
            Text(stringResource(R.string.settings_reading_style), style = MaterialTheme.typography.titleMedium)
            ToggleSettingRow(
                title = stringResource(R.string.settings_keep_screen_on),
                description = stringResource(R.string.settings_keep_screen_on_description),
                checked = readingStyle.keepScreenOn,
                onCheckedChange = onKeepScreenOnChanged,
            )
            ToggleSettingRow(
                title = stringResource(R.string.settings_multitouch_zoom),
                description = stringResource(R.string.settings_multitouch_zoom_description),
                checked = readingStyle.multitouchZoomEnabled,
                onCheckedChange = onMultitouchZoomEnabledChanged,
            )
            ToggleSettingRow(
                title = stringResource(R.string.settings_bold_text),
                description = stringResource(R.string.settings_bold_text_description),
                checked = readingStyle.boldTextEnabled,
                onCheckedChange = onBoldTextEnabledChanged,
            )
            ToggleSettingRow(
                title = stringResource(R.string.settings_note_icon),
                description = stringResource(R.string.settings_note_icon_description),
                checked = readingStyle.showNotesInReader,
                onCheckedChange = onShowNotesInReaderChanged,
            )
            SettingSlider(
                title = stringResource(R.string.settings_font_size),
                valueLabel = "${readingStyle.fontSizeSp.roundToInt()}sp",
                value = readingStyle.fontSizeSp,
                valueRange = 12f..28f,
                steps = 15,
                onValueChange = onFontSizeChanged,
            )
            SettingSlider(
                title = stringResource(R.string.settings_line_spacing),
                valueLabel = "${(readingStyle.lineHeightMultiplier * 100).roundToInt()}%",
                value = readingStyle.lineHeightMultiplier,
                valueRange = 1.2f..2.2f,
                steps = 9,
                onValueChange = onLineHeightChanged,
            )
            Text(stringResource(R.string.settings_palette), style = MaterialTheme.typography.labelLarge)
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
        label = { Text(stringResource(palette.labelRes())) },
    )
}
