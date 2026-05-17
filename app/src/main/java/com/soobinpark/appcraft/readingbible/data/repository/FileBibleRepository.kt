package com.soobinpark.appcraft.readingbible.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.soobinpark.appcraft.readingbible.data.biblefile.BibleFileScanner
import com.soobinpark.appcraft.readingbible.data.biblefile.SafBibleFileScanner
import com.soobinpark.appcraft.readingbible.data.cache.BibleChapterCache
import com.soobinpark.appcraft.readingbible.data.parser.BdfBibleFileParser
import com.soobinpark.appcraft.readingbible.data.parser.LfaBibleFileParser
import com.soobinpark.appcraft.readingbible.data.parser.SafBdfBibleFileParser
import com.soobinpark.appcraft.readingbible.data.parser.SafLfaBibleFileParser
import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleSourceType
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.repository.BibleRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FileBibleRepository(
    private val scanner: BibleFileScanner = BibleFileScanner(),
    private val bdfParser: BdfBibleFileParser = BdfBibleFileParser(),
    private val lfaParser: LfaBibleFileParser = LfaBibleFileParser(),
    private val safScanner: SafBibleFileScanner? = null,
    private val safBdfParser: SafBdfBibleFileParser? = null,
    private val safLfaParser: SafLfaBibleFileParser? = null,
    private val chapterPersistentCache: BibleChapterCache? = null,
    private val context: Context? = null,
) : BibleRepository {
    constructor(context: Context) : this(
        safScanner = SafBibleFileScanner(context),
        safBdfParser = SafBdfBibleFileParser(context),
        safLfaParser = SafLfaBibleFileParser(context),
        chapterPersistentCache = BibleChapterCache(context.applicationContext),
        context = context.applicationContext,
    )

    override suspend fun scanVersions(root: File): List<BibleVersion> {
        val key = "file:${root.absolutePath}:${root.lastModified()}"
        return versionCache.getOrPut(key) {
            chapterPersistentCache?.readVersions(scanKey = key, fileRoot = root)?.takeIf { it.isNotEmpty() }
                ?: scanner.scan(root).also { chapterPersistentCache?.writeVersions(key, it) }
        }
    }

    suspend fun scanVersions(treeUri: Uri): List<BibleVersion> {
        val key = "saf:$treeUri"
        return versionCache.getOrPut(key) {
            chapterPersistentCache?.readVersions(scanKey = key, treeUri = treeUri)?.takeIf { it.isNotEmpty() }
                ?: safScanner?.scan(treeUri).orEmpty().also { chapterPersistentCache?.writeVersions(key, it) }
        }
    }

    override suspend fun readChapter(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse> {
        val key = listOf(
            version.treeUri?.toString() ?: version.fileRoot?.absolutePath.orEmpty(),
            version.code,
            version.sourceType.name,
            sourceStamp(version, book),
            book.index,
            chapter,
        ).joinToString(":")
        return chapterCache.getOrPut(key) {
            chapterPersistentCache?.readChapter(key)?.takeIf { it.isNotEmpty() }?.let { return@getOrPut it }
            if (version.treeUri != null) {
                when (version.sourceType) {
                    BibleSourceType.BdfSplit -> safBdfParser?.readChapter(version, book, chapter).orEmpty()
                    BibleSourceType.LfaArchive -> safLfaParser?.readChapter(version, book, chapter).orEmpty()
                }
            } else {
                when (version.sourceType) {
                    BibleSourceType.BdfSplit -> bdfParser.readChapter(version, book, chapter)
                    BibleSourceType.LfaArchive -> lfaParser.readChapter(version, book, chapter)
                }
            }
                .also { chapterPersistentCache?.writeChapter(key, it) }
        }
    }

    suspend fun warmUpVersion(
        version: BibleVersion,
        onProgress: suspend (current: Int, total: Int, label: String) -> Unit = { _, _, _ -> },
    ) {
        val warmUpKey = listOf(
            version.treeUri?.toString() ?: version.fileRoot?.absolutePath.orEmpty(),
            version.code,
            version.sourceType.name,
            "full",
        ).joinToString(":")
        if (!warmUpVersions.add(warmUpKey)) return
        val total = BibleCatalog.books.sumOf { it.chapterCount }
        var current = 0
        for (book in BibleCatalog.books) {
            for (chapter in 1..book.chapterCount) {
                current += 1
                if (current == 1 || current == total || chapter == 1 || current % 25 == 0) {
                    onProgress(current, total, "${book.koreanName} ${chapter}장")
                }
                readChapter(version, book, chapter)
            }
        }
    }

    private fun sourceStamp(
        version: BibleVersion,
        book: BibleBook,
    ): String {
        return if (version.treeUri != null) {
            safSourceStamp(version, book)
        } else {
            fileSourceStamp(version, book)
        }
    }

    private fun fileSourceStamp(
        version: BibleVersion,
        book: BibleBook,
    ): String {
        val root = version.fileRoot ?: return "missing"
        val file = when (version.sourceType) {
            BibleSourceType.BdfSplit -> File(root, "${version.code}${BibleCatalog.fileIndexForBook(book.index)}.bdf")
            BibleSourceType.LfaArchive -> File(root, "${version.code}.lfa")
        }
        return "${file.lastModified()}:${file.length()}"
    }

    private fun safSourceStamp(
        version: BibleVersion,
        book: BibleBook,
    ): String {
        val appContext = context ?: return "saf"
        val treeUri = version.treeUri ?: return "missing"
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return "missing"
        val fileName = when (version.sourceType) {
            BibleSourceType.BdfSplit -> "${version.code}${BibleCatalog.fileIndexForBook(book.index)}.bdf"
            BibleSourceType.LfaArchive -> "${version.code}.lfa"
        }
        val file = root.findFile(fileName) ?: return "missing"
        return "${file.lastModified()}:${file.length()}"
    }

    companion object {
        private val versionCache = ConcurrentHashMap<String, List<BibleVersion>>()
        private val chapterCache = ConcurrentHashMap<String, List<BibleVerse>>()
        private val warmUpVersions = ConcurrentHashMap.newKeySet<String>()
    }
}
