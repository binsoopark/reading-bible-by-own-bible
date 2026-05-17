package com.soobinpark.appcraft.readingbible.domain.model

import android.net.Uri
import java.io.File

enum class BibleSourceType {
    BdfSplit,
    LfaArchive,
}

data class BibleVersion(
    val code: String,
    val displayName: String,
    val sourceType: BibleSourceType,
    val fileRoot: File? = null,
    val treeUri: Uri? = null,
)

data class BibleBook(
    val index: Int,
    val koreanName: String,
    val englishName: String,
    val chapterCount: Int,
)

data class BibleVerse(
    val versionCode: String,
    val bookIndex: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
)

data class ReadingSelection(
    val version: BibleVersion?,
    val book: BibleBook,
    val chapter: Int,
)
