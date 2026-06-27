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
import com.soobinpark.appcraft.readingbible.data.parser.decodeLfaText
import com.soobinpark.appcraft.readingbible.data.parser.parseVerseLine
import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleSourceType
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.repository.BibleRepository
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

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
        val key = scanKey(root)
        return versionCache.getOrPut(key) {
            chapterPersistentCache?.readVersions(scanKey = key, fileRoot = root)?.takeIf { it.isNotEmpty() }
                ?: scanner.scan(root).also { chapterPersistentCache?.writeVersions(key, it) }
        }
    }

    suspend fun scanVersions(treeUri: Uri): List<BibleVersion> {
        val key = scanKey(treeUri)
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
        val key = chapterCacheKey(version, book, chapter)
        return chapterCache.getOrPut(key) {
            chapterPersistentCache?.readChapter(key)?.takeIf { it.isNotEmpty() }?.let { return@getOrPut it }
            runCatching {
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
            }.getOrDefault(emptyList())
                .also { verses -> runCatching { chapterPersistentCache?.writeChapter(key, verses) } }
        }
    }

    suspend fun warmUpVersion(
        version: BibleVersion,
        onProgress: suspend (current: Int, total: Int, label: String) -> Unit = { _, _, _ -> },
    ) {
        val warmUpKey = warmUpKey(version)
        if (chapterPersistentCache?.isWarmUpComplete(warmUpKey) == true) return
        if (!warmUpVersions.add(warmUpKey)) return
        var completed = false
        try {
            val total = BibleCatalog.books.sumOf { it.chapterCount }
            var current = 0
            val chapters = readAllChaptersForWarmUp(version).onEach { (_, verses) ->
                val first = verses.firstOrNull()
                if (first != null) {
                    current += 1
                    val book = BibleCatalog.books[first.bookIndex]
                    if (current == 1 || current == total || first.chapter == 1 || current % 25 == 0) {
                        onProgress(current, total, "${book.koreanName} ${first.chapter}장")
                    }
                }
            }
            onProgress(current.coerceAtLeast(1), total, "DB 캐시에 저장하는 중입니다")
            chapterPersistentCache?.writeChapters(chapters)
            chapterCache.putAll(chapters)
            onProgress(total, total, "캐시 저장 완료")
            chapterPersistentCache?.markWarmUpComplete(warmUpKey)
            completed = true
        } finally {
            if (!completed) {
                warmUpVersions.remove(warmUpKey)
            }
        }
    }

    fun searchCached(
        version: BibleVersion,
        query: String,
    ): List<BibleSearchResult>? {
        val cached = chapterPersistentCache?.searchChapters(
            cacheKeyPrefix = chapterCachePrefix(version),
            query = query,
        ) ?: return null
        if (cached.searchedChapters.isEmpty()) return null
        return cached.results
    }

    suspend fun searchOptimized(
        version: BibleVersion,
        query: String,
        onProgress: suspend (current: Int, total: Int, label: String) -> Unit = { _, _, _ -> },
    ): List<BibleSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return emptyList()

        val cached = chapterPersistentCache?.searchChapters(chapterCachePrefix(version), normalizedQuery)
        val results = cached?.results.orEmpty().toMutableList()
        val searchedChapters = cached?.searchedChapters.orEmpty()
        val allChapters = BibleCatalog.books.flatMap { book ->
            (1..book.chapterCount).map { chapter -> book to chapter }
        }
        val missingChapters = allChapters.filterNot { (book, chapter) ->
            BibleChapterCache.ChapterRef(book.index, chapter) in searchedChapters
        }
        missingChapters.forEachIndexed { index, (book, chapter) ->
            if (index == 0 || index == missingChapters.lastIndex || chapter == 1 || index % 25 == 0) {
                onProgress(index + 1, missingChapters.size.coerceAtLeast(1), "${book.koreanName} ${chapter}장")
            }
            readChapter(version, book, chapter)
                .filter { verse -> verse.text.contains(normalizedQuery, ignoreCase = true) }
                .forEach { verse ->
                    results += BibleSearchResult(
                        verse = verse,
                        book = book,
                        snippet = verse.text,
                    )
                }
        }
        return results.sortedWith(
            compareBy<BibleSearchResult> { it.verse.bookIndex }
                .thenBy { it.verse.chapter }
                .thenBy { it.verse.verse },
        )
    }

    private fun readAllChaptersForWarmUp(version: BibleVersion): Map<String, List<BibleVerse>> {
        return runCatching {
            if (version.treeUri != null) {
                when (version.sourceType) {
                    BibleSourceType.BdfSplit -> readAllSafBdfChapters(version)
                    BibleSourceType.LfaArchive -> readAllSafLfaChapters(version)
                }
            } else {
                when (version.sourceType) {
                    BibleSourceType.BdfSplit -> readAllBdfChapters(version)
                    BibleSourceType.LfaArchive -> readAllLfaChapters(version)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun readAllBdfChapters(version: BibleVersion): Map<String, List<BibleVerse>> {
        val root = version.fileRoot ?: return emptyMap()
        val chapters = mutableMapOf<ChapterTarget, MutableList<BibleVerse>>()
        for (fileIndex in 1..7) {
            val file = File(root, "${version.code}$fileIndex.bdf")
            if (!file.exists()) continue
            parseBdfText(version, file.readBytes().decodeBibleText(), fileIndex, chapters)
        }
        return chapters.toCacheMap(version)
    }

    private fun readAllSafBdfChapters(version: BibleVersion): Map<String, List<BibleVerse>> {
        val appContext = context ?: return emptyMap()
        val treeUri = version.treeUri ?: return emptyMap()
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return emptyMap()
        val chapters = mutableMapOf<ChapterTarget, MutableList<BibleVerse>>()
        for (fileIndex in 1..7) {
            val file = root.findFile("${version.code}$fileIndex.bdf") ?: continue
            val bytes = appContext.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } ?: continue
            parseBdfText(version, bytes.decodeBibleText(), fileIndex, chapters)
        }
        return chapters.toCacheMap(version)
    }

    private fun parseBdfText(
        version: BibleVersion,
        text: String,
        fileIndex: Int,
        chapters: MutableMap<ChapterTarget, MutableList<BibleVerse>>,
    ) {
        val books = BibleCatalog.books.filter { BibleCatalog.fileIndexForBook(it.index) == fileIndex }
        val bookByNumber = books.associateBy { BibleCatalog.bookNumber(it.index) }
        text.lineSequence().forEach { line ->
            val verse = parseAnyVerseLine(version.code, bookByNumber, line) ?: return@forEach
            chapters.getOrPut(ChapterTarget(verse.bookIndex, verse.chapter)) { mutableListOf() }.add(verse)
        }
    }

    private fun readAllLfaChapters(version: BibleVersion): Map<String, List<BibleVerse>> {
        val root = version.fileRoot ?: return emptyMap()
        val archive = File(root, "${version.code}.lfa")
        if (!archive.exists()) return emptyMap()
        val chapters = mutableMapOf<ChapterTarget, MutableList<BibleVerse>>()
        ZipFile(archive).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val target = parseLfaEntryName(version.code, entry.name) ?: continue
                val text = decodeLfaText(zip.getInputStream(entry).readBytes())
                parseLfaText(version, target, text, chapters)
            }
        }
        return chapters.toCacheMap(version)
    }

    private fun readAllSafLfaChapters(version: BibleVersion): Map<String, List<BibleVerse>> {
        val appContext = context ?: return emptyMap()
        val treeUri = version.treeUri ?: return emptyMap()
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return emptyMap()
        val archive = root.findFile("${version.code}.lfa") ?: return emptyMap()
        val chapters = mutableMapOf<ChapterTarget, MutableList<BibleVerse>>()
        val input = appContext.contentResolver.openInputStream(archive.uri) ?: return emptyMap()
        input.use { stream ->
            ZipInputStream(stream.buffered()).use { zip ->
                while (true) {
                    val entry = runCatching { zip.nextEntry }.getOrNull() ?: break
                    val target = parseLfaEntryName(version.code, entry.name) ?: continue
                    val text = runCatching { decodeLfaText(zip.readBytes()) }.getOrNull() ?: continue
                    parseLfaText(version, target, text, chapters)
                }
            }
        }
        return chapters.toCacheMap(version)
    }

    private fun parseLfaText(
        version: BibleVersion,
        target: ChapterTarget,
        text: String,
        chapters: MutableMap<ChapterTarget, MutableList<BibleVerse>>,
    ) {
        val book = BibleCatalog.books[target.bookIndex]
        val bookNumber = BibleCatalog.bookNumber(book.index)
        val chapterNeedle = " ${target.chapter}:"
        val verses = text.lineSequence()
            .mapNotNull { line -> parseVerseLine(version.code, book.index, target.chapter, bookNumber, chapterNeedle, line) }
            .toList()
        if (verses.isNotEmpty()) {
            chapters.getOrPut(target) { mutableListOf() }.addAll(verses)
        }
    }

    private fun Map<ChapterTarget, List<BibleVerse>>.toCacheMap(version: BibleVersion): Map<String, List<BibleVerse>> {
        val sourceStamps = warmUpSourceStampsByBook(version)
        return mapKeys { (target, _) ->
            val book = BibleCatalog.books[target.bookIndex]
            chapterCacheKey(version, book, target.chapter, sourceStamps[book.index] ?: sourceStamp(version, book))
        }.mapValues { (_, verses) -> verses.sortedBy { it.verse } }
    }

    private fun parseLfaEntryName(
        versionCode: String,
        entryName: String,
    ): ChapterTarget? {
        if (!entryName.startsWith(versionCode) || !entryName.endsWith(".lfb", ignoreCase = true)) return null
        val token = entryName.removePrefix(versionCode).removeSuffix(".lfb")
        val bookNumber = token.substringBefore("_")
        val chapter = token.substringAfter("_", missingDelimiterValue = "").toIntOrNull() ?: return null
        val bookIndex = bookNumber.toIntOrNull()?.minus(1) ?: return null
        if (bookIndex !in BibleCatalog.books.indices) return null
        if (chapter !in 1..BibleCatalog.books[bookIndex].chapterCount) return null
        return ChapterTarget(bookIndex, chapter)
    }

    private fun parseAnyVerseLine(
        versionCode: String,
        bookByNumber: Map<String, BibleBook>,
        line: String,
    ): BibleVerse? {
        val bookNumber = line.take(2)
        val book = bookByNumber[bookNumber] ?: return null
        val afterBook = line.drop(2).trimStart()
        val chapter = afterBook.substringBefore(":", missingDelimiterValue = "").trim().toIntOrNull() ?: return null
        if (chapter !in 1..book.chapterCount) return null
        val afterColon = afterBook.substringAfter(":", missingDelimiterValue = "").trim()
        val verseNumber = afterColon.substringBefore(" ").toIntOrNull() ?: return null
        val text = afterColon.substringAfter(" ", missingDelimiterValue = "").trim()
        if (text.isBlank()) return null
        return BibleVerse(
            versionCode = versionCode,
            bookIndex = book.index,
            chapter = chapter,
            verse = verseNumber,
            text = text,
        )
    }

    private fun ByteArray.decodeBibleText(): String {
        val charsets = listOf("MS949", "UTF-8", "UTF-16", "ISO-8859-7")
        for (charsetName in charsets) {
            runCatching {
                return toString(Charset.forName(charsetName))
            }
        }
        return decodeToString()
    }

    private fun chapterCacheKey(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): String {
        return chapterCacheKey(version, book, chapter, sourceStamp(version, book))
    }

    private fun chapterCacheKey(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
        sourceStamp: String,
    ): String {
        return listOf(
            version.treeUri?.toString() ?: version.fileRoot?.absolutePath.orEmpty(),
            version.code,
            version.sourceType.name,
            sourceStamp,
            book.index,
            chapter,
        ).joinToString(":")
    }

    private fun chapterCachePrefix(version: BibleVersion): String {
        return listOf(
            version.treeUri?.toString() ?: version.fileRoot?.absolutePath.orEmpty(),
            version.code,
            version.sourceType.name,
        ).joinToString(":") + ":"
    }

    private fun warmUpSourceStampsByBook(version: BibleVersion): Map<Int, String> {
        return when (version.sourceType) {
            BibleSourceType.LfaArchive -> {
                val stamp = lfaSourceStamp(version)
                BibleCatalog.books.associate { it.index to stamp }
            }
            BibleSourceType.BdfSplit -> {
                val stampsByFileIndex = (1..7).associateWith { fileIndex -> bdfSourceStamp(version, fileIndex) }
                BibleCatalog.books.associate { book ->
                    book.index to stampsByFileIndex.getValue(BibleCatalog.fileIndexForBook(book.index))
                }
            }
        }
    }

    private fun lfaSourceStamp(version: BibleVersion): String {
        return if (version.treeUri != null) {
            safFileSourceStamp(version, "${version.code}.lfa")
        } else {
            val root = version.fileRoot ?: return "missing"
            val file = File(root, "${version.code}.lfa")
            "${file.lastModified()}:${file.length()}"
        }
    }

    private fun bdfSourceStamp(
        version: BibleVersion,
        fileIndex: Int,
    ): String {
        return if (version.treeUri != null) {
            safFileSourceStamp(version, "${version.code}$fileIndex.bdf")
        } else {
            val root = version.fileRoot ?: return "missing"
            val file = File(root, "${version.code}$fileIndex.bdf")
            "${file.lastModified()}:${file.length()}"
        }
    }

    private fun safFileSourceStamp(
        version: BibleVersion,
        fileName: String,
    ): String {
        val appContext = context ?: return "saf"
        val treeUri = version.treeUri ?: return "missing"
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return "missing"
        val file = root.findFile(fileName) ?: return "missing"
        return "${file.lastModified()}:${file.length()}"
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

    private fun warmUpKey(version: BibleVersion): String {
        return listOf(
            version.treeUri?.toString() ?: version.fileRoot?.absolutePath.orEmpty(),
            version.code,
            version.sourceType.name,
            versionSourceStamp(version),
            "full",
        ).joinToString(":")
    }

    private fun versionSourceStamp(version: BibleVersion): String {
        return if (version.treeUri != null) {
            safVersionSourceStamp(version)
        } else {
            fileVersionSourceStamp(version)
        }
    }

    private fun fileVersionSourceStamp(version: BibleVersion): String {
        val root = version.fileRoot ?: return "missing"
        val files = when (version.sourceType) {
            BibleSourceType.BdfSplit -> (1..7).map { File(root, "${version.code}$it.bdf") }
            BibleSourceType.LfaArchive -> listOf(File(root, "${version.code}.lfa"))
        }
        return files.joinToString("|") { "${it.name}:${it.lastModified()}:${it.length()}" }
    }

    private fun safVersionSourceStamp(version: BibleVersion): String {
        val appContext = context ?: return "saf"
        val treeUri = version.treeUri ?: return "missing"
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return "missing"
        val fileNames = when (version.sourceType) {
            BibleSourceType.BdfSplit -> (1..7).map { "${version.code}$it.bdf" }
            BibleSourceType.LfaArchive -> listOf("${version.code}.lfa")
        }
        return fileNames.joinToString("|") { fileName ->
            val file = root.findFile(fileName)
            "$fileName:${file?.lastModified() ?: 0L}:${file?.length() ?: 0L}"
        }
    }

    private fun scanKey(root: File): String {
        val fileStamp = root.listFiles()
            ?.filter { it.isFile && (it.extension.equals("bdf", ignoreCase = true) || it.extension.equals("lfa", ignoreCase = true)) }
            ?.sortedBy { it.name.lowercase() }
            ?.joinToString("|") { "${it.name}:${it.lastModified()}:${it.length()}" }
            .orEmpty()
        return "file:${root.absolutePath}:$fileStamp"
    }

    private fun scanKey(treeUri: Uri): String {
        val appContext = context ?: return "saf:$treeUri"
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return "saf:$treeUri:missing"
        val fileStamp = root.listFiles()
            .filter { it.isFile && (it.name?.endsWith(".bdf", ignoreCase = true) == true || it.name?.endsWith(".lfa", ignoreCase = true) == true) }
            .sortedBy { it.name.orEmpty().lowercase() }
            .joinToString("|") { "${it.name}:${it.lastModified()}:${it.length()}" }
        return "saf:$treeUri:$fileStamp"
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

    private data class ChapterTarget(
        val bookIndex: Int,
        val chapter: Int,
    )
}
