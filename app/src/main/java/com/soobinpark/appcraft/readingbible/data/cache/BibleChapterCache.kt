package com.soobinpark.appcraft.readingbible.data.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
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
        db.execSQL(
            """
            CREATE TABLE versions (
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

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        db.execSQL("DROP TABLE IF EXISTS versions")
        db.execSQL("DROP TABLE IF EXISTS verses")
        db.execSQL("DROP TABLE IF EXISTS chapters")
        onCreate(db)
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
                            displayName = it.getString(1),
                            sourceType = BibleSourceType.valueOf(it.getString(2)),
                            fileRoot = fileRoot,
                            treeUri = treeUri,
                        ),
                    )
                } while (it.moveToNext())
            }
        }
    }

    @Synchronized
    fun writeVersions(
        scanKey: String,
        versions: List<BibleVersion>,
    ) {
        if (versions.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("versions", "scan_key = ?", arrayOf(scanKey))
            versions.forEachIndexed { index, version ->
                db.insertOrThrow(
                    "versions",
                    null,
                    ContentValues().apply {
                        put("scan_key", scanKey)
                        put("code", version.code)
                        put("display_name", version.displayName)
                        put("source_type", version.sourceType.name)
                        put("position", index)
                    },
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
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

    @Synchronized
    fun writeChapter(
        cacheKey: String,
        verses: List<BibleVerse>,
    ) {
        val first = verses.firstOrNull() ?: return
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("verses", "cache_key = ?", arrayOf(cacheKey))
            db.delete("chapters", "cache_key = ?", arrayOf(cacheKey))
            db.insertOrThrow(
                "chapters",
                null,
                ContentValues().apply {
                    put("cache_key", cacheKey)
                    put("version_code", first.versionCode)
                    put("book_index", first.bookIndex)
                    put("chapter", first.chapter)
                    put("cached_at", System.currentTimeMillis())
                },
            )
            verses.forEach { verse ->
                db.insertOrThrow(
                    "verses",
                    null,
                    ContentValues().apply {
                        put("cache_key", cacheKey)
                        put("verse", verse.verse)
                        put("text", verse.text)
                    },
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private data class ChapterInfo(
        val versionCode: String,
        val bookIndex: Int,
        val chapter: Int,
    )

    private companion object {
        const val DATABASE_NAME = "bible_chapter_cache.db"
        const val DATABASE_VERSION = 2
    }
}
