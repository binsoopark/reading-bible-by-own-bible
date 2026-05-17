package com.soobinpark.appcraft.readingbible.domain.model

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
    val bdfFileCount: Int,
    val completeBdfVersionCount: Int,
    val unknownFileCount: Int,
    val versions: List<BibleVersion>,
    val issues: List<BibleFileIssue>,
)
