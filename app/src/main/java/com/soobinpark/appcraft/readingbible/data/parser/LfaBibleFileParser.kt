package com.soobinpark.appcraft.readingbible.data.parser

import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File
import java.util.zip.ZipFile

class LfaBibleFileParser : BibleFileParser {
    override suspend fun readChapter(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse> {
        val root = version.fileRoot ?: return emptyList()
        val archive = File(root, "${version.code}.lfa")
        if (!archive.exists()) return emptyList()

        val bookNumber = BibleCatalog.bookNumber(book.index)
        val chapterNeedle = " $chapter:"
        val entryName = "${version.code}${bookNumber}_$chapter.lfb"

        return ZipFile(archive).use { zip ->
            val entry = zip.getEntry(entryName) ?: return emptyList()
            zip.getInputStream(entry).bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    parseVerseLine(version.code, book.index, chapter, bookNumber, chapterNeedle, line)
                }.toList()
            }
        }
    }
}
