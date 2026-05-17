package com.soobinpark.appcraft.readingbible.data.parser

import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File
import java.nio.charset.Charset

class BdfBibleFileParser : BibleFileParser {
    override suspend fun readChapter(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse> {
        val fileIndex = BibleCatalog.fileIndexForBook(book.index)
        val root = version.fileRoot ?: return emptyList()
        val file = File(root, "${version.code}$fileIndex.bdf")
        if (!file.exists()) return emptyList()

        val bookNumber = BibleCatalog.bookNumber(book.index)
        val chapterNeedle = " $chapter:"

        return readLinesWithFallbackCharset(file)
            .asSequence()
            .mapNotNull { line -> parseVerseLine(version.code, book.index, chapter, bookNumber, chapterNeedle, line) }
            .toList()
    }
}

internal fun parseVerseLine(
    versionCode: String,
    bookIndex: Int,
    chapter: Int,
    bookNumber: String,
    chapterNeedle: String,
    line: String,
): BibleVerse? {
    if (!line.startsWith(bookNumber)) return null
    if (!line.contains(chapterNeedle)) return null
    if (line.indexOf(chapterNeedle) >= line.indexOf(":") + 1) return null

    val afterColon = line.substringAfter(":", missingDelimiterValue = "").trim()
    val verseNumber = afterColon.substringBefore(" ").toIntOrNull() ?: return null
    val text = afterColon.substringAfter(" ", missingDelimiterValue = "").trim()
    if (text.isBlank()) return null

    return BibleVerse(
        versionCode = versionCode,
        bookIndex = bookIndex,
        chapter = chapter,
        verse = verseNumber,
        text = text,
    )
}

private fun readLinesWithFallbackCharset(file: File): List<String> {
    val charsets = listOf("MS949", "UTF-8", "UTF-16", "ISO-8859-7")
    for (charsetName in charsets) {
        runCatching {
            return file.readLines(Charset.forName(charsetName))
        }
    }
    return file.readLines()
}
