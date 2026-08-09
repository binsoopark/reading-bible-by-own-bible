package com.soobinpark.appcraft.readingbible.domain.model

enum class RecordFilter {
    Bookmark,
    Highlight,
    Read,
    Note,
    ;

    fun matches(bookmark: VerseBookmark): Boolean {
        return when (this) {
            Bookmark -> bookmark.isBookmarked
            Highlight -> bookmark.highlight != VerseHighlight.None
            Read -> bookmark.isRead
            Note -> bookmark.note.isNotBlank()
        }
    }
}

enum class RecordSortOrder {
    Recent,
    Bible,
}

object RecordShareFormatter {
    fun format(
        bookmarks: List<VerseBookmark>,
        bookName: (BibleBook) -> String = { it.koreanName },
        verseLine: (Int, String) -> String = { verse, text -> "$verse $text" },
    ): String {
        if (bookmarks.isEmpty()) return ""
        val sorted = bookmarks.sortedWith(
            compareBy<VerseBookmark>({ it.versionCode.lowercase() }, { it.bookIndex }, { it.chapter }, { it.verse }),
        )
        val sections = mutableListOf<String>()
        var currentKey: Triple<String, Int, Int>? = null
        val sectionBookmarks = mutableListOf<VerseBookmark>()

        fun flush() {
            if (sectionBookmarks.isEmpty()) return
            val first = sectionBookmarks.first()
            val localizedBookName = BibleCatalog.books.getOrNull(first.bookIndex)?.let(bookName).orEmpty()
            val verseLabel = sectionBookmarks.map { it.verse }.toVerseRangeLabel()
            val reference = "$localizedBookName ${first.chapter}:$verseLabel · ${first.versionCode}".trim()
            sections += if (sectionBookmarks.size == 1) {
                "“${first.text.trim()}”\n$reference"
            } else {
                val body = sectionBookmarks.joinToString("\n") { verseLine(it.verse, it.text.trim()) }
                "$body\n\n$reference"
            }
            sectionBookmarks.clear()
        }

        sorted.forEach { bookmark ->
            val key = Triple(bookmark.versionCode.lowercase(), bookmark.bookIndex, bookmark.chapter)
            if (currentKey != null && key != currentKey) {
                flush()
            }
            currentKey = key
            sectionBookmarks += bookmark
        }
        flush()
        return sections.joinToString("\n\n")
    }

    private fun List<Int>.toVerseRangeLabel(): String {
        if (isEmpty()) return ""
        val sortedVerses = distinct().sorted()
        val ranges = mutableListOf<String>()
        var start = sortedVerses.first()
        var end = start
        sortedVerses.drop(1).forEach { verse ->
            if (verse == end + 1) {
                end = verse
            } else {
                ranges += if (start == end) "$start" else "$start-$end"
                start = verse
                end = verse
            }
        }
        ranges += if (start == end) "$start" else "$start-$end"
        return ranges.joinToString(", ")
    }
}

fun VerseBookmark.shouldPersist(): Boolean {
    return isBookmarked || note.isNotBlank() || highlight != VerseHighlight.None || isRead
}
