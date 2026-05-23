package com.soobinpark.appcraft.readingbible.domain.model

enum class VerseHighlight(
    val label: String,
) {
    None("없음"),
    Yellow("노랑"),
    Mint("민트"),
    Blue("파랑"),
    Pink("분홍"),
    Lavender("라벤더"),
    Orange("주황"),
}

data class VerseBookmark(
    val versionCode: String,
    val bookIndex: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val note: String = "",
    val isBookmarked: Boolean = true,
    val highlight: VerseHighlight = VerseHighlight.None,
    val isRead: Boolean = false,
    val createdAtMillis: Long,
) {
    val key: String = key(versionCode, bookIndex, chapter, verse)

    companion object {
        fun key(
            versionCode: String,
            bookIndex: Int,
            chapter: Int,
            verse: Int,
        ): String = listOf(versionCode, bookIndex, chapter, verse).joinToString(":")
    }
}
