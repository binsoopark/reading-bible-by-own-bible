package com.soobinpark.appcraft.readingbible.domain.usecase

import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.app.AppText
import com.soobinpark.appcraft.readingbible.domain.model.BibleCatalog
import com.soobinpark.appcraft.readingbible.domain.model.BibleSearchResult
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.model.localizedName
import java.util.Locale
import com.soobinpark.appcraft.readingbible.domain.repository.BibleRepository

class SearchBibleUseCase(
    private val repository: BibleRepository,
) {
    private val verseIndex = mutableMapOf<String, List<IndexedSearchResult>>()

    suspend operator fun invoke(
        version: BibleVersion,
        query: String,
        onProgress: suspend (current: Int, total: Int, label: String) -> Unit = { _, _, _ -> },
    ): List<BibleSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return emptyList()

        val indexedVerses = verseIndex.getOrPut(version.indexKey()) {
            buildIndex(version, onProgress)
        }
        val normalizedNeedle = normalizedQuery.lowercase()
        return indexedVerses
            .asSequence()
            .filter { it.normalizedText.contains(normalizedNeedle) }
            .map { it.result }
            .toList()
    }

    private suspend fun buildIndex(
        version: BibleVersion,
        onProgress: suspend (current: Int, total: Int, label: String) -> Unit,
    ): List<IndexedSearchResult> {
        val total = BibleCatalog.books.sumOf { it.chapterCount }
        val results = mutableListOf<IndexedSearchResult>()
        var current = 0
        for (book in BibleCatalog.books) {
            for (chapter in 1..book.chapterCount) {
                current += 1
                if (current == 1 || current == total || chapter == 1 || current % 25 == 0) {
                    onProgress(current, total, AppText.get(R.string.format_chapter, book.localizedName(Locale.getDefault().language), chapter))
                }
                val verses = repository.readChapter(version, book, chapter)
                verses.forEach { verse ->
                    val result = BibleSearchResult(
                            verse = verse,
                            book = book,
                            snippet = verse.text,
                        )
                    results += IndexedSearchResult(
                        result = result,
                        normalizedText = verse.text.lowercase(),
                    )
                }
            }
        }
        return results
    }

    private fun BibleVersion.indexKey(): String {
        return "${treeUri ?: fileRoot?.absolutePath.orEmpty()}:$code:$sourceType"
    }

    private data class IndexedSearchResult(
        val result: BibleSearchResult,
        val normalizedText: String,
    )
}
