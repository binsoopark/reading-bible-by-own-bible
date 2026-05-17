package com.soobinpark.appcraft.readingbible.domain.model

import android.net.Uri

enum class BibleFileIssueSeverity {
    Info,
    Warning,
    Error,
}

data class BibleFileIssue(
    val title: String,
    val detail: String,
    val severity: BibleFileIssueSeverity,
)

data class BibleFileDiagnostic(
    val rootLabel: String,
    val scannedFileCount: Int,
    val lfaCount: Int,
    val lfbCount: Int,
    val mp3Count: Int,
    val bdfFileCount: Int,
    val completeBdfVersionCount: Int,
    val unknownFileCount: Int,
    val versions: List<BibleVersion>,
    val audioFiles: List<BibleAudioFile>,
    val issues: List<BibleFileIssue>,
)

data class BibleAudioFile(
    val name: String,
    val uri: Uri,
)
