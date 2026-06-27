package com.soobinpark.appcraft.readingbible.domain.usecase

import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.repository.BibleRepository

class ReadChapterUseCase(
    private val repository: BibleRepository,
) {
    suspend operator fun invoke(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse> = repository.readChapter(version, book, chapter)
}
