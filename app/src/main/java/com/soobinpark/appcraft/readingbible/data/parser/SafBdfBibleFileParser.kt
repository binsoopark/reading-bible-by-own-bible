package com.soobinpark.appcraft.readingbible.data.parser

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.nio.charset.Charset

class SafBdfBibleFileParser(
    private val context: Context,
) : BibleFileParser {
    override suspend fun readChapter(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse> {
        val treeUri = version.treeUri ?: return emptyList()
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val fileIndex = BibleCatalog.fileIndexForBook(book.index)
        val file = root.findFile("${version.code}$fileIndex.bdf") ?: return emptyList()

        val bookNumber = BibleCatalog.bookNumber(book.index)
        val chapterNeedle = " $chapter:"

        return readTextWithFallbackCharset(file)
            .lineSequence()
            .mapNotNull { line -> parseVerseLine(version.code, book.index, chapter, bookNumber, chapterNeedle, line) }
            .toList()
    }

    private fun readTextWithFallbackCharset(file: DocumentFile): String {
        val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } ?: return ""
        val charsets = listOf("MS949", "UTF-8", "UTF-16", "ISO-8859-7")
        for (charsetName in charsets) {
            runCatching {
                return bytes.toString(Charset.forName(charsetName))
            }
        }
        return bytes.decodeToString()
    }
}
