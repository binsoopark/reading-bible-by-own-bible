package com.soobinpark.appcraft.readingbible.domain.model

enum class RecordFilter(val label: String) {
    Bookmark("북마크"),
    Highlight("형광펜"),
    Read("읽음"),
    Note("메모"),
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

object RecordShareFormatter {
    fun format(bookmarks: List<VerseBookmark>): String {
        if (bookmarks.isEmpty()) return ""
        val sorted = bookmarks.sortedWith(compareBy({ it.bookIndex }, { it.chapter }, { it.verse }))
        val sections = mutableListOf<String>()
        var currentHeader = ""
        val lines = mutableListOf<String>()

        fun flush() {
            if (lines.isEmpty()) return
            sections += listOf(currentHeader, lines.joinToString("\n")).filter { it.isNotBlank() }.joinToString("\n")
            lines.clear()
        }

        sorted.forEach { bookmark ->
            val book = BibleCatalog.books.getOrNull(bookmark.bookIndex)
            val header = "${book?.koreanName ?: ""} ${bookmark.chapter}장"
            if (header != currentHeader) {
                flush()
                currentHeader = header
            }
            lines += "${bookmark.verse}절 ${bookmark.text}"
        }
        flush()
        return sections.joinToString("\n\n")
    }
}

fun VerseBookmark.shouldPersist(): Boolean {
    return isBookmarked || note.isNotBlank() || highlight != VerseHighlight.None || isRead
}
