package com.soobinpark.appcraft.readingbible.data.parser

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.util.zip.ZipInputStream

class SafLfaBibleFileParser(
    private val context: Context,
) : BibleFileParser {
    override suspend fun readChapter(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse> {
        val treeUri = version.treeUri ?: return emptyList()
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val archive = root.findFile("${version.code}.lfa") ?: return emptyList()

        val bookNumber = BibleCatalog.bookNumber(book.index)
        val chapterNeedle = " $chapter:"
        val entryName = "${version.code}${bookNumber}_$chapter.lfb"

        val input = context.contentResolver.openInputStream(archive.uri) ?: return emptyList()
        return ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { it.name == entryName }
                ?.let {
                    val text = decodeLfaText(zip.readBytes())
                    text.lineSequence().mapNotNull { line ->
                            parseVerseLine(version.code, book.index, chapter, bookNumber, chapterNeedle, line)
                        }.toList()
                }
                .orEmpty()
        }
    }
}
