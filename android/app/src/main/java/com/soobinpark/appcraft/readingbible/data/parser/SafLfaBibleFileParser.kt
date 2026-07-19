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
        return input.use { stream ->
            ZipInputStream(stream.buffered()).use { zip ->
                while (true) {
                    val entry = runCatching { zip.nextEntry }.getOrNull() ?: return emptyList()
                    if (entry.name.equals(entryName, ignoreCase = true)) {
                        val text = runCatching { decodeLfaText(zip.readBytes()) }.getOrNull() ?: return emptyList()
                        return text.lineSequence().mapNotNull { line ->
                            parseVerseLine(version.code, book.index, chapter, bookNumber, chapterNeedle, line)
                        }.toList()
                    }
                }
                emptyList()
            }
        }
    }
}
