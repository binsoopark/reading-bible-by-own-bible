package com.soobinpark.appcraft.readingbible.data.preference

import android.content.Context
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight
import com.soobinpark.appcraft.readingbible.domain.model.VerseBookmark
import com.soobinpark.appcraft.readingbible.domain.model.shouldPersist
import org.json.JSONArray
import org.json.JSONObject

class BookmarkPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reading_bible_preferences", Context.MODE_PRIVATE)

    fun getBookmarks(): List<VerseBookmark> {
        val raw = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return fromJson(raw)
    }

    fun fromJson(raw: String): List<VerseBookmark> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        VerseBookmark(
                            versionCode = item.optString("versionCode"),
                            bookIndex = item.optInt("bookIndex"),
                            chapter = item.optInt("chapter"),
                            verse = item.optInt("verse"),
                            text = item.optString("text"),
                            note = item.optString("note"),
                            isBookmarked = item.optBoolean("isBookmarked", true),
                            highlight = item.optString("highlight", VerseHighlight.None.name)
                                .let { runCatching { VerseHighlight.valueOf(it) }.getOrDefault(VerseHighlight.None) },
                            isRead = item.optBoolean("isRead", false),
                            createdAtMillis = item.optLong("createdAtMillis"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
            .filter { it.versionCode.isNotBlank() && it.text.isNotBlank() }
            .filter { it.shouldPersist() }
            .distinctBy { it.key }
            .sortedByDescending { it.createdAtMillis }
    }

    fun setBookmarks(bookmarks: List<VerseBookmark>) {
        prefs.edit()
            .putString(KEY_BOOKMARKS, toJson(bookmarks))
            .apply()
    }

    fun toJson(bookmarks: List<VerseBookmark>): String {
        val array = JSONArray()
        bookmarks
            .distinctBy { it.key }
            .sortedByDescending { it.createdAtMillis }
            .forEach { bookmark ->
                array.put(
                    JSONObject()
                        .put("versionCode", bookmark.versionCode)
                        .put("bookIndex", bookmark.bookIndex)
                        .put("chapter", bookmark.chapter)
                        .put("verse", bookmark.verse)
                        .put("text", bookmark.text)
                        .put("note", bookmark.note)
                        .put("isBookmarked", bookmark.isBookmarked)
                        .put("highlight", bookmark.highlight.name)
                        .put("isRead", bookmark.isRead)
                        .put("createdAtMillis", bookmark.createdAtMillis),
                )
            }
        return array.toString()
    }

    private companion object {
        const val KEY_BOOKMARKS = "verse_bookmarks"
    }
}
