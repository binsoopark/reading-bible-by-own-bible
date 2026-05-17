package com.soobinpark.appcraft.readingbible.data.biblefile

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
                    title = "기본 폴더가 없습니다",
                    detail = "${root.absolutePath} 폴더를 찾지 못했습니다. 설정 탭에서 bdf/lfa 폴더를 선택해 주세요.",
                    severity = BibleFileIssueSeverity.Error,
                ),
            )
        }
        if (!root.isDirectory) {
            return emptyDiagnostic(
                rootLabel = root.absolutePath,
                issue = BibleFileIssue(
                    title = "폴더가 아닙니다",
                    detail = "${root.absolutePath} 경로가 파일로 감지되었습니다.",
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
                    title = "선택한 폴더를 열 수 없습니다",
                    detail = "저장소 권한이 사라졌거나 폴더가 이동되었을 수 있습니다. 설정 탭에서 폴더를 다시 선택해 주세요.",
                    severity = BibleFileIssueSeverity.Error,
                ),
            )

        if (!root.isDirectory) {
            return emptyDiagnostic(
                rootLabel = root.name ?: treeUri.toString(),
                issue = BibleFileIssue(
                    title = "폴더가 아닙니다",
                    detail = "선택된 URI가 폴더로 인식되지 않습니다.",
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
                title = "폴더가 비어 있습니다",
                detail = "이 폴더에서 bdf/lfa 파일을 찾을 수 없습니다.",
                severity = BibleFileIssueSeverity.Warning,
            )
        }

        groups.forEach { (prefix, indexes) ->
            val missing = (1..7).filterNot { it in indexes }
            if (missing.isNotEmpty()) {
                issues += BibleFileIssue(
                    title = "$prefix bdf 파일이 일부 없습니다",
                    detail = "누락된 분할 파일: ${missing.joinToString { "$prefix$it.bdf" }}",
                    severity = BibleFileIssueSeverity.Warning,
                )
            }
        }

        if (lfbCount > 0) {
            issues += BibleFileIssue(
                title = ".lfb 파일이 감지되었습니다",
                detail = "현재 MVP는 .bdf와 .lfa 읽기를 우선 지원합니다. .lfb는 이후 단계에서 구조 분석 후 연결할 예정입니다.",
                severity = BibleFileIssueSeverity.Info,
            )
        }

        if (mp3Count > 0) {
            issues += BibleFileIssue(
                title = "MP3 파일이 감지되었습니다",
                detail = "오디오 파일 ${mp3Count}개가 있습니다. 현재 단계에서는 감지만 지원하고, 재생 UI는 이후 오디오 플레이어 단계에서 연결합니다.",
                severity = BibleFileIssueSeverity.Info,
            )
        }

        val unknownFileCount = fileNames.size - knownCount
        if (unknownFileCount > 0) {
            issues += BibleFileIssue(
                title = "알 수 없는 파일이 있습니다",
                detail = "지원 포맷이 아닌 파일 ${unknownFileCount}개가 함께 있습니다.",
                severity = BibleFileIssueSeverity.Info,
            )
        }

        if (versions.isEmpty() && fileNames.isNotEmpty()) {
            issues += BibleFileIssue(
                title = "읽을 수 있는 역본이 없습니다",
                detail = ".lfa 파일이 없거나, .bdf 파일이 1~7번 세트로 완성되지 않았을 수 있습니다.",
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
