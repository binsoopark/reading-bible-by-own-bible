package com.soobinpark.appcraft.readingbible.domain.usecase

import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.repository.BibleRepository

class SearchBibleUseCase(
    private val repository: BibleRepository,
) {
    private val verseIndex = mutableMapOf<String, List<BibleSearchResult>>()

    suspend operator fun invoke(
        version: BibleVersion,
        query: String,
        limit: Int = 100,
    ): List<BibleSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return emptyList()

        val indexedVerses = verseIndex.getOrPut(version.indexKey()) {
            buildIndex(version)
        }
        return indexedVerses
            .filter { it.verse.text.contains(normalizedQuery, ignoreCase = true) }
            .take(limit)
    }

    private suspend fun buildIndex(version: BibleVersion): List<BibleSearchResult> {
        val results = mutableListOf<BibleSearchResult>()
        for (book in BibleCatalog.books) {
            for (chapter in 1..book.chapterCount) {
                val verses = repository.readChapter(version, book, chapter)
                verses.forEach { verse ->
                    results += BibleSearchResult(
                        verse = verse,
                        book = book,
                        snippet = verse.text,
                    )
                }
            }
        }
        return results
    }

    private fun BibleVersion.indexKey(): String {
        return "${treeUri ?: fileRoot?.absolutePath.orEmpty()}:$code:$sourceType"
    }
}
