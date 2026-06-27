package com.soobinpark.appcraft.readingbible.data.biblefile

import com.soobinpark.appcraft.readingbible.domain.model.BibleSourceType
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File

class BibleFileScanner {
    fun scan(root: File): List<BibleVersion> {
        if (!root.exists() || !root.isDirectory) return emptyList()

        val lfaVersions = root.listFiles()
            ?.filter { it.isFile && it.extension.equals("lfa", ignoreCase = true) }
            ?.map { file ->
                BibleVersion(
                    code = file.nameWithoutExtension,
                    displayName = BibleVersionNames.displayNameFor(file.nameWithoutExtension),
                    sourceType = BibleSourceType.LfaArchive,
                    fileRoot = root,
                )
            }
            .orEmpty()

        val bdfPrefixes = root.listFiles()
            ?.filter { it.isFile && it.extension.equals("bdf", ignoreCase = true) }
            ?.mapNotNull { file -> file.nameWithoutExtension.dropLastWhile { it.isDigit() }.takeIf { it.isNotBlank() } }
            ?.distinct()
            ?.filter { prefix -> (1..7).all { File(root, "$prefix$it.bdf").exists() } }
            ?.map { code ->
                BibleVersion(
                    code = code,
                    displayName = BibleVersionNames.displayNameFor(code),
                    sourceType = BibleSourceType.BdfSplit,
                    fileRoot = root,
                )
            }
            .orEmpty()

        return (lfaVersions + bdfPrefixes)
            .distinctBy { it.code.lowercase() }
            .sortedBy { it.code.lowercase() }
    }
}
