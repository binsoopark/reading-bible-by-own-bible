package com.soobinpark.appcraft.readingbible.data.biblefile

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.soobinpark.appcraft.readingbible.domain.model.BibleSourceType
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion

class SafBibleFileScanner(
    private val context: Context,
) {
    fun scan(treeUri: Uri): List<BibleVersion> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val files = root.listFiles().filter { it.isFile }

        val lfaVersions = files
            .filter { it.name?.endsWith(".lfa", ignoreCase = true) == true }
            .mapNotNull { file ->
                file.name?.removeSuffixIgnoreCase(".lfa")?.let { code ->
                    BibleVersion(
                        code = code,
                        displayName = BibleVersionNames.displayNameFor(code),
                        sourceType = BibleSourceType.LfaArchive,
                        treeUri = treeUri,
                    )
                }
            }

        val bdfPrefixes = files
            .mapNotNull { file -> file.name }
            .filter { it.endsWith(".bdf", ignoreCase = true) }
            .map { it.substringBeforeLast(".").dropLastWhile { char -> char.isDigit() } }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { prefix -> (1..7).all { index -> root.findFile("$prefix$index.bdf") != null } }
            .map { code ->
                BibleVersion(
                    code = code,
                    displayName = BibleVersionNames.displayNameFor(code),
                    sourceType = BibleSourceType.BdfSplit,
                    treeUri = treeUri,
                )
            }

        return (lfaVersions + bdfPrefixes)
            .distinctBy { it.code.lowercase() }
            .sortedBy { it.code.lowercase() }
    }
}

private fun String.removeSuffixIgnoreCase(suffix: String): String {
    return if (endsWith(suffix, ignoreCase = true)) dropLast(suffix.length) else this
}
