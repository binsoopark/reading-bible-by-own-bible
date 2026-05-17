package com.soobinpark.appcraft.readingbible.domain.repository

import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File

interface BibleRepository {
    suspend fun scanVersions(root: File): List<BibleVersion>

    suspend fun readChapter(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse>
}
