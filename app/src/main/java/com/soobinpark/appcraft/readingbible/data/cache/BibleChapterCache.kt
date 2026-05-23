package com.soobinpark.appcraft.readingbible.data.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import com.soobinpark.appcraft.readingbible.data.biblefile.BibleVersionNames
import com.soobinpark.appcraft.readingbible.domain.model.BibleSourceType
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import java.io.File

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
                cached_at INTEGER NOT NULL
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
            arrayOf("version_code", "book_index", "chapter"),
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
            )
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
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                verses.forEach { verse ->
                    db.insertWithOnConflict(
                        "verses",
                        null,
                        ContentValues().apply {
                            put("cache_key", cacheKey)
                            put("verse", verse.verse)
                            put("text", verse.text)
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

    fun writeChapters(chapters: Map<String, List<BibleVerse>>) {
        if (chapters.isEmpty()) return
        synchronized(databaseWriteLock) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val deleteVerses = db.compileStatement("DELETE FROM verses WHERE cache_key = ?")
                val deleteChapter = db.compileStatement("DELETE FROM chapters WHERE cache_key = ?")
                val insertChapter = db.compileStatement(
                    """
                    INSERT OR REPLACE INTO chapters (
                        cache_key,
                        version_code,
                        book_index,
                        chapter,
                        cached_at
                    ) VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                )
                val insertVerse = db.compileStatement(
                    """
                    INSERT OR REPLACE INTO verses (
                        cache_key,
                        verse,
                        text
                    ) VALUES (?, ?, ?)
                    """.trimIndent(),
                )
                val now = System.currentTimeMillis()
                chapters.forEach { (cacheKey, verses) ->
                    val first = verses.firstOrNull() ?: return@forEach
                    deleteVerses.bindString(1, cacheKey)
                    deleteVerses.executeUpdateDelete()
                    deleteVerses.clearBindings()

                    deleteChapter.bindString(1, cacheKey)
                    deleteChapter.executeUpdateDelete()
                    deleteChapter.clearBindings()

                    insertChapter.bindString(1, cacheKey)
                    insertChapter.bindString(2, first.versionCode)
                    insertChapter.bindLong(3, first.bookIndex.toLong())
                    insertChapter.bindLong(4, first.chapter.toLong())
                    insertChapter.bindLong(5, now)
                    insertChapter.executeInsert()
                    insertChapter.clearBindings()

                    verses.forEach { verse ->
                        insertVerse.bindString(1, cacheKey)
                        insertVerse.bindLong(2, verse.verse.toLong())
                        insertVerse.bindString(3, verse.text)
                        insertVerse.executeInsert()
                        insertVerse.clearBindings()
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

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
    )

    private companion object {
        const val DATABASE_NAME = "bible_chapter_cache.db"
        const val DATABASE_VERSION = 3
        val databaseWriteLock = Any()
    }
}
