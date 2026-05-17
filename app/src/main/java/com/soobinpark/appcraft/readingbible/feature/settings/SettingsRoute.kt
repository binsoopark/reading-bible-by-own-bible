package com.soobinpark.appcraft.readingbible.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soobinpark.appcraft.readingbible.R

@Composable
fun SettingsRoute(
    dataFolderUri: Uri?,
    onDataFolderSelected: (Uri?) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("성경 데이터 폴더", style = MaterialTheme.typography.titleMedium)
                Text(dataFolderUri?.toString() ?: "아직 선택한 폴더가 없습니다. 선택하지 않으면 기본 /sdcard/bible 폴더를 시도합니다.")
                Button(onClick = { folderPicker.launch(null) }) {
                    Text("bdf/lfa 폴더 선택")
                }
            }
        }
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("앱 정보", style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.app_info_lifove_compat))
            }
        }
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("다음 단계", style = MaterialTheme.typography.titleMedium)
                Text("데이터 폴더 선택, 읽기 팔레트, 글자 크기, 줄 간격 설정을 이 화면에 추가합니다.")
            }
        }
    }
}
