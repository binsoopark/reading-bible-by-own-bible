package com.soobinpark.appcraft.readingbible.data.biblefile

import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.app.AppText

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.soobinpark.appcraft.readingbible.domain.model.BibleAudioFile
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileDiagnostic
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileIssue
import com.soobinpark.appcraft.readingbible.domain.model.BibleFileIssueSeverity
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File

class BibleFileDiagnosticsReader(
    private val context: Context,
) {
    private val fileScanner = BibleFileScanner()
    private val safScanner = SafBibleFileScanner(context)

    fun diagnose(root: File): BibleFileDiagnostic {
        if (!root.exists()) {
            return emptyDiagnostic(
                rootLabel = root.absolutePath,
                issue = BibleFileIssue(
                    title = AppText.get(R.string.diagnostic_default_missing_title),
                    detail = AppText.get(R.string.diagnostic_default_missing_detail, root.absolutePath),
                    severity = BibleFileIssueSeverity.Error,
                ),
            )
        }
        if (!root.isDirectory) {
            return emptyDiagnostic(
                rootLabel = root.absolutePath,
                issue = BibleFileIssue(
                    title = AppText.get(R.string.diagnostic_not_folder_title),
                    detail = AppText.get(R.string.diagnostic_path_is_file, root.absolutePath),
                    severity = BibleFileIssueSeverity.Error,
                ),
            )
        }

        val files = root.listFiles()?.filter { it.isFile }.orEmpty()
        return buildDiagnostic(
            rootLabel = root.absolutePath,
            fileNames = files.map { it.name },
            audioFiles = files
                .filter { it.extension.equals("mp3", ignoreCase = true) }
                .map { BibleAudioFile(name = it.name, uri = Uri.fromFile(it)) },
            versions = fileScanner.scan(root),
        )
    }

    fun diagnose(treeUri: Uri): BibleFileDiagnostic {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return emptyDiagnostic(
                rootLabel = treeUri.toString(),
                issue = BibleFileIssue(
                    title = AppText.get(R.string.diagnostic_cannot_open_title),
                    detail = AppText.get(R.string.diagnostic_cannot_open_detail),
                    severity = BibleFileIssueSeverity.Error,
                ),
            )

        if (!root.isDirectory) {
            return emptyDiagnostic(
                rootLabel = root.name ?: treeUri.toString(),
                issue = BibleFileIssue(
                    title = AppText.get(R.string.diagnostic_not_folder_title),
                    detail = AppText.get(R.string.diagnostic_uri_not_folder),
                    severity = BibleFileIssueSeverity.Error,
                ),
            )
        }

        val files = root.listFiles().filter { it.isFile }
        return buildDiagnostic(
            rootLabel = root.name ?: treeUri.toString(),
            fileNames = files.mapNotNull { it.name },
            audioFiles = files
                .filter { it.name?.endsWith(".mp3", ignoreCase = true) == true }
                .mapNotNull { file -> file.name?.let { BibleAudioFile(name = it, uri = file.uri) } },
            versions = safScanner.scan(treeUri),
        )
    }

    private fun buildDiagnostic(
        rootLabel: String,
        fileNames: List<String>,
        audioFiles: List<BibleAudioFile>,
        versions: List<BibleVersion>,
    ): BibleFileDiagnostic {
        val lfaCount = fileNames.count { it.endsWith(".lfa", ignoreCase = true) }
        val lfbCount = fileNames.count { it.endsWith(".lfb", ignoreCase = true) }
        val mp3Count = fileNames.count { it.endsWith(".mp3", ignoreCase = true) }
        val bdfNames = fileNames.filter { it.endsWith(".bdf", ignoreCase = true) }
        val knownCount = lfaCount + lfbCount + mp3Count + bdfNames.size
        val groups = bdfNames
            .mapNotNull { name ->
                val stem = name.substringBeforeLast(".")
                val prefix = stem.dropLastWhile { it.isDigit() }
                val index = stem.takeLastWhile { it.isDigit() }.toIntOrNull()
                if (prefix.isBlank() || index == null) null else prefix to index
            }
            .groupBy({ it.first }, { it.second })

        val completeBdfVersionCount = groups.count { (_, indexes) -> (1..7).all { it in indexes } }
        val issues = mutableListOf<BibleFileIssue>()

        if (fileNames.isEmpty()) {
            issues += BibleFileIssue(
                title = AppText.get(R.string.diagnostic_empty_title),
                detail = AppText.get(R.string.diagnostic_empty_detail),
                severity = BibleFileIssueSeverity.Warning,
            )
        }

        groups.forEach { (prefix, indexes) ->
            val missing = (1..7).filterNot { it in indexes }
            if (missing.isNotEmpty()) {
                issues += BibleFileIssue(
                    title = AppText.get(R.string.diagnostic_incomplete_bdf_title, prefix),
                    detail = AppText.get(R.string.diagnostic_missing_files, missing.joinToString { "$prefix$it.bdf" }),
                    severity = BibleFileIssueSeverity.Warning,
                )
            }
        }

        if (lfbCount > 0) {
            issues += BibleFileIssue(
                title = AppText.get(R.string.diagnostic_lfb_title),
                detail = AppText.get(R.string.diagnostic_lfb_detail),
                severity = BibleFileIssueSeverity.Info,
            )
        }

        if (mp3Count > 0) {
            issues += BibleFileIssue(
                title = AppText.get(R.string.diagnostic_mp3_title),
                detail = AppText.get(R.string.diagnostic_mp3_detail, mp3Count),
                severity = BibleFileIssueSeverity.Info,
            )
        }

        val unknownFileCount = fileNames.size - knownCount
        if (unknownFileCount > 0) {
            issues += BibleFileIssue(
                title = AppText.get(R.string.diagnostic_unknown_title),
                detail = AppText.get(R.string.diagnostic_unknown_detail, unknownFileCount),
                severity = BibleFileIssueSeverity.Info,
            )
        }

        if (versions.isEmpty() && fileNames.isNotEmpty()) {
            issues += BibleFileIssue(
                title = AppText.get(R.string.diagnostic_no_readable_title),
                detail = AppText.get(R.string.diagnostic_no_readable_detail),
                severity = BibleFileIssueSeverity.Error,
            )
        }

        return BibleFileDiagnostic(
            rootLabel = rootLabel,
            scannedFileCount = fileNames.size,
            lfaCount = lfaCount,
            lfbCount = lfbCount,
            mp3Count = mp3Count,
            bdfFileCount = bdfNames.size,
            completeBdfVersionCount = completeBdfVersionCount,
            unknownFileCount = unknownFileCount,
            versions = versions,
            audioFiles = audioFiles,
            issues = issues.sortedByDescending { it.severity.ordinal },
        )
    }

    private fun emptyDiagnostic(
        rootLabel: String,
        issue: BibleFileIssue,
    ): BibleFileDiagnostic {
        return BibleFileDiagnostic(
            rootLabel = rootLabel,
            scannedFileCount = 0,
            lfaCount = 0,
            lfbCount = 0,
            mp3Count = 0,
            bdfFileCount = 0,
            completeBdfVersionCount = 0,
            unknownFileCount = 0,
            versions = emptyList(),
            audioFiles = emptyList(),
            issues = listOf(issue),
        )
    }
}
