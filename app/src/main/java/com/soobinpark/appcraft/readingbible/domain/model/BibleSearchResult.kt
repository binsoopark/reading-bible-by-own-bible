package com.soobinpark.appcraft.readingbible.domain.model

data class BibleSearchResult(
    val verse: BibleVerse,
    val book: BibleBook,
    val snippet: String,
)
