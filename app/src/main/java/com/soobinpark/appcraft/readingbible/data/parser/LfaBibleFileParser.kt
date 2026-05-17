package com.soobinpark.appcraft.readingbible.data.parser

import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File
import java.nio.charset.Charset
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
            val text = decodeLfaText(zip.getInputStream(entry).readBytes())
            text.lineSequence().mapNotNull { line ->
                    parseVerseLine(version.code, book.index, chapter, bookNumber, chapterNeedle, line)
                }.toList()
        }
    }
}

internal fun decodeLfaText(bytes: ByteArray): String {
    val utf8 = bytes.toString(Charsets.UTF_8)
    return if (utf8.count { it == '\uFFFD' } > 3) {
        bytes.toString(Charset.forName("MS949"))
    } else {
        utf8
    }
}
