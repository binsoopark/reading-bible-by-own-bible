package com.soobinpark.appcraft.readingbible.domain.usecase

import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.repository.BibleRepository

class SearchBibleUseCase(
    private val repository: BibleRepository,
) {
    suspend operator fun invoke(
        version: BibleVersion,
        query: String,
        limit: Int = 100,
    ): List<BibleSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return emptyList()

        val results = mutableListOf<BibleSearchResult>()
        for (book in BibleCatalog.books) {
            for (chapter in 1..book.chapterCount) {
                val verses = repository.readChapter(version, book, chapter)
                verses.forEach { verse ->
                    if (verse.text.contains(normalizedQuery, ignoreCase = true)) {
                        results += BibleSearchResult(
                            verse = verse,
                            book = book,
                            snippet = verse.text,
                        )
                        if (results.size >= limit) return results
                    }
                }
            }
        }
        return results
    }
}
