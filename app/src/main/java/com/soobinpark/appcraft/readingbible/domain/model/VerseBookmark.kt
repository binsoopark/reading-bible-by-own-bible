package com.soobinpark.appcraft.readingbible.domain.model

data class VerseBookmark(
    val versionCode: String,
    val bookIndex: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val note: String = "",
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
