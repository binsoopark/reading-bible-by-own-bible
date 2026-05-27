package com.soobinpark.appcraft.readingbible.data.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Base64
import com.soobinpark.appcraft.readingbible.data.biblefile.BibleVersionNames
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleSourceType
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File
import java.nio.charset.StandardCharsets

class BibleChapterCache(
    context: Context,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE chapters (
                cache_key TEXT PRIMARY KEY,
                version_code TEXT NOT NULL,
                book_index INTEGER NOT NULL,
                chapter INTEGER NOT NULL,
                cached_at INTEGER NOT NULL,
                verses_blob TEXT,
                search_text TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE verses (
                cache_key TEXT NOT NULL,
                verse INTEGER NOT NULL,
                text TEXT NOT NULL,
                PRIMARY KEY (cache_key, verse),
                FOREIGN KEY (cache_key) REFERENCES chapters(cache_key) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX index_verses_cache_key ON verses(cache_key)")
        createVersionsTable(db)
        createWarmUpsTable(db)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        if (oldVersion < 2) {
            createVersionsTable(db)
        }
        if (oldVersion < 3) {
            createWarmUpsTable(db)
        }
        if (oldVersion < 4) {
            addVersesBlobColumn(db)
        }
        if (oldVersion < 5) {
            addSearchTextColumn(db)
        }
        if (oldVersion > newVersion) {
            db.execSQL("DROP TABLE IF EXISTS warmups")
            db.execSQL("DROP TABLE IF EXISTS versions")
            db.execSQL("DROP TABLE IF EXISTS verses")
            db.execSQL("DROP TABLE IF EXISTS chapters")
            onCreate(db)
        }
    }

    private fun createVersionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS versions (
                scan_key TEXT NOT NULL,
                code TEXT NOT NULL,
                display_name TEXT NOT NULL,
                source_type TEXT NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY (scan_key, code, source_type)
            )
            """.trimIndent(),
        )
    }

    private fun addVersesBlobColumn(db: SQLiteDatabase) {
        runCatching {
            db.execSQL("ALTER TABLE chapters ADD COLUMN verses_blob TEXT")
        }
    }

    private fun addSearchTextColumn(db: SQLiteDatabase) {
        runCatching {
            db.execSQL("ALTER TABLE chapters ADD COLUMN search_text TEXT")
        }
        backfillSearchText(db)
    }

    private fun backfillSearchText(db: SQLiteDatabase) {
        val cursor = db.query(
            "chapters",
            arrayOf("cache_key", "verses_blob"),
            "verses_blob IS NOT NULL AND search_text IS NULL",
            null,
            null,
            null,
            null,
        )
        cursor.use {
            if (!it.moveToFirst()) return
            val statement = db.compileStatement("UPDATE chapters SET search_text = ? WHERE cache_key = ?")
            do {
                val cacheKey = it.getString(0)
                val searchText = decodeSearchTextFromBlob(it.getString(1))
                statement.bindString(1, searchText)
                statement.bindString(2, cacheKey)
                statement.executeUpdateDelete()
                statement.clearBindings()
            } while (it.moveToNext())
        }
    }

    private fun createWarmUpsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS warmups (
                warmup_key TEXT PRIMARY KEY,
                completed_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    @Synchronized
    fun readVersions(
        scanKey: String,
        fileRoot: File? = null,
        treeUri: Uri? = null,
    ): List<BibleVersion>? {
        val cursor = readableDatabase.query(
            "versions",
            arrayOf("code", "display_name", "source_type"),
            "scan_key = ?",
            arrayOf(scanKey),
            null,
            null,
            "position ASC",
        )
        return cursor.use {
            if (!it.moveToFirst()) return null
            buildList {
                do {
                    add(
                        BibleVersion(
                            code = it.getString(0),
                            displayName = BibleVersionNames.displayNameFor(it.getString(0)),
                            sourceType = BibleSourceType.valueOf(it.getString(2)),
                            fileRoot = fileRoot,
                            treeUri = treeUri,
                        ),
                    )
                } while (it.moveToNext())
            }
        }
    }

    fun writeVersions(
        scanKey: String,
        versions: List<BibleVersion>,
    ) {
        if (versions.isEmpty()) return
        synchronized(databaseWriteLock) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                db.delete("versions", "scan_key = ?", arrayOf(scanKey))
                versions.forEachIndexed { index, version ->
                    db.insertWithOnConflict(
                        "versions",
                        null,
                        ContentValues().apply {
                            put("scan_key", scanKey)
                            put("code", version.code)
                            put("display_name", version.displayName)
                            put("source_type", version.sourceType.name)
                            put("position", index)
                        },
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    @Synchronized
    fun readChapter(cacheKey: String): List<BibleVerse>? {
        val db = readableDatabase
        val chapterCursor = db.query(
            "chapters",
            arrayOf("version_code", "book_index", "chapter", "verses_blob"),
            "cache_key = ?",
            arrayOf(cacheKey),
            null,
            null,
            null,
            "1",
        )
        val chapterInfo = chapterCursor.use { cursor ->
            if (!cursor.moveToFirst()) return null
            ChapterInfo(
                versionCode = cursor.getString(0),
                bookIndex = cursor.getInt(1),
                chapter = cursor.getInt(2),
                versesBlob = cursor.getString(3),
            )
        }
        chapterInfo.versesBlob?.takeIf { it.isNotBlank() }?.let { blob ->
            decodeVersesBlob(chapterInfo, blob)?.let { return it }
        }

        val verseCursor = db.query(
            "verses",
            arrayOf("verse", "text"),
            "cache_key = ?",
            arrayOf(cacheKey),
            null,
            null,
            "verse ASC",
        )
        return verseCursor.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        BibleVerse(
                            versionCode = chapterInfo.versionCode,
                            bookIndex = chapterInfo.bookIndex,
                            chapter = chapterInfo.chapter,
                            verse = cursor.getInt(0),
                            text = cursor.getString(1),
                        ),
                    )
                }
            }
        }
    }

    fun writeChapter(
        cacheKey: String,
        verses: List<BibleVerse>,
    ) {
        val first = verses.firstOrNull() ?: return
        synchronized(databaseWriteLock) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                db.delete("verses", "cache_key = ?", arrayOf(cacheKey))
                db.delete("chapters", "cache_key = ?", arrayOf(cacheKey))
                db.insertWithOnConflict(
                    "chapters",
                    null,
                    ContentValues().apply {
                        put("cache_key", cacheKey)
                        put("version_code", first.versionCode)
                        put("book_index", first.bookIndex)
                        put("chapter", first.chapter)
                        put("cached_at", System.currentTimeMillis())
                        put("verses_blob", encodeVersesBlob(verses))
                        put("search_text", encodeSearchText(verses))
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun writeChapters(chapters: Map<String, List<BibleVerse>>) {
        if (chapters.isEmpty()) return
        synchronized(databaseWriteLock) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val deleteVerses = db.compileStatement("DELETE FROM verses WHERE cache_key = ?")
                val insertChapter = db.compileStatement(
                    """
                    INSERT OR REPLACE INTO chapters (
                        cache_key,
                        version_code,
                        book_index,
                        chapter,
                        cached_at,
                        verses_blob,
                        search_text
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                )
                val now = System.currentTimeMillis()
                chapters.forEach { (cacheKey, verses) ->
                    val first = verses.firstOrNull() ?: return@forEach
                    deleteVerses.bindString(1, cacheKey)
                    deleteVerses.executeUpdateDelete()
                    deleteVerses.clearBindings()

                    insertChapter.bindString(1, cacheKey)
                    insertChapter.bindString(2, first.versionCode)
                    insertChapter.bindLong(3, first.bookIndex.toLong())
                    insertChapter.bindLong(4, first.chapter.toLong())
                    insertChapter.bindLong(5, now)
                    insertChapter.bindString(6, encodeVersesBlob(verses))
                    insertChapter.bindString(7, encodeSearchText(verses))
                    insertChapter.executeInsert()
                    insertChapter.clearBindings()
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    @Synchronized
    fun searchChapters(
        cacheKeyPrefix: String,
        query: String,
    ): CachedSearchResult {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return CachedSearchResult()
        val prefixPattern = "${cacheKeyPrefix.escapeLikePattern()}%"
        val queryPattern = "%${normalizedQuery.escapeLikePattern()}%"
        val db = readableDatabase

        val refCursor = db.query(
            "chapters",
            arrayOf("book_index", "chapter"),
            "cache_key LIKE ? ESCAPE '\\' AND search_text IS NOT NULL",
            arrayOf(prefixPattern),
            null,
            null,
            "book_index ASC, chapter ASC",
        )
        val searchedChapters = refCursor.use {
            buildSet {
                while (it.moveToNext()) {
                    add(ChapterRef(it.getInt(0), it.getInt(1)))
                }
            }
        }
        if (searchedChapters.isEmpty()) return CachedSearchResult()

        val cursor = db.query(
            "chapters",
            arrayOf("version_code", "book_index", "chapter", "verses_blob"),
            "cache_key LIKE ? ESCAPE '\\' AND search_text IS NOT NULL AND search_text LIKE ? ESCAPE '\\'",
            arrayOf(prefixPattern, queryPattern),
            null,
            null,
            "book_index ASC, chapter ASC",
        )
        return cursor.use {
            val results = buildList {
                while (it.moveToNext()) {
                    val chapterInfo = ChapterInfo(
                        versionCode = it.getString(0),
                        bookIndex = it.getInt(1),
                        chapter = it.getInt(2),
                        versesBlob = it.getString(3),
                    )
                    val book = BibleCatalog.books.getOrNull(chapterInfo.bookIndex) ?: continue
                    decodeVersesBlob(chapterInfo, chapterInfo.versesBlob.orEmpty())
                        ?.filter { verse -> verse.text.contains(normalizedQuery, ignoreCase = true) }
                        ?.forEach { verse ->
                            add(
                                BibleSearchResult(
                                    verse = verse,
                                    book = book,
                                    snippet = verse.text,
                                ),
                            )
                        }
                }
            }
            CachedSearchResult(
                results = results,
                searchedChapters = searchedChapters,
            )
        }
    }

    data class CachedSearchResult(
        val results: List<BibleSearchResult> = emptyList(),
        val searchedChapters: Set<ChapterRef> = emptySet(),
    )

    data class ChapterRef(
        val bookIndex: Int,
        val chapter: Int,
    )

    @Synchronized
    fun isWarmUpComplete(warmUpKey: String): Boolean {
        val cursor = readableDatabase.query(
            "warmups",
            arrayOf("warmup_key"),
            "warmup_key = ?",
            arrayOf(warmUpKey),
            null,
            null,
            null,
            "1",
        )
        return cursor.use { it.moveToFirst() }
    }

    fun markWarmUpComplete(warmUpKey: String) {
        synchronized(databaseWriteLock) {
            writableDatabase.insertWithOnConflict(
                "warmups",
                null,
                ContentValues().apply {
                    put("warmup_key", warmUpKey)
                    put("completed_at", System.currentTimeMillis())
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    private data class ChapterInfo(
        val versionCode: String,
        val bookIndex: Int,
        val chapter: Int,
        val versesBlob: String?,
    )

    private companion object {
        const val DATABASE_NAME = "bible_chapter_cache.db"
        const val DATABASE_VERSION = 5
        val databaseWriteLock = Any()

        fun encodeVersesBlob(verses: List<BibleVerse>): String {
            return verses.joinToString("\n") { verse ->
                val text = Base64.encodeToString(verse.text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                "${verse.verse}\t$text"
            }
        }

        fun encodeSearchText(verses: List<BibleVerse>): String {
            return verses.joinToString("\n") { it.text }
        }

        fun decodeSearchTextFromBlob(blob: String?): String {
            if (blob.isNullOrBlank()) return ""
            return decodeVersesBlob(
                chapterInfo = ChapterInfo(
                    versionCode = "",
                    bookIndex = 0,
                    chapter = 1,
                    versesBlob = blob,
                ),
                blob = blob,
            )?.joinToString("\n") { it.text }.orEmpty()
        }

        fun decodeVersesBlob(
            chapterInfo: ChapterInfo,
            blob: String,
        ): List<BibleVerse>? {
            return runCatching {
                blob.lineSequence()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        val verse = line.substringBefore("\t").toIntOrNull() ?: return@mapNotNull null
                        val encodedText = line.substringAfter("\t", missingDelimiterValue = "")
                        val text = Base64.decode(encodedText, Base64.NO_WRAP).toString(StandardCharsets.UTF_8)
                        BibleVerse(
                            versionCode = chapterInfo.versionCode,
                            bookIndex = chapterInfo.bookIndex,
                            chapter = chapterInfo.chapter,
                            verse = verse,
                            text = text,
                        )
                    }
                    .toList()
            }.getOrNull()
        }

        fun String.escapeLikePattern(): String {
            return buildString(length) {
                this@escapeLikePattern.forEach { char ->
                    when (char) {
                        '\\', '%', '_' -> append('\\')
                    }
                    append(char)
                }
            }
        }
    }
}
