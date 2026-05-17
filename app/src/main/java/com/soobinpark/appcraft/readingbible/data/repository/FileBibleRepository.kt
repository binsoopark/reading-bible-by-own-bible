package com.soobinpark.appcraft.readingbible.data.repository

import android.content.Context
import android.net.Uri
import com.soobinpark.appcraft.readingbible.data.biblefile.BibleFileScanner
import com.soobinpark.appcraft.readingbible.data.biblefile.SafBibleFileScanner
import com.soobinpark.appcraft.readingbible.data.parser.BdfBibleFileParser
import com.soobinpark.appcraft.readingbible.data.parser.LfaBibleFileParser
import com.soobinpark.appcraft.readingbible.data.parser.SafBdfBibleFileParser
import com.soobinpark.appcraft.readingbible.data.parser.SafLfaBibleFileParser
import com.soobinpark.appcraft.readingbible.domain.model.BibleBook
import com.soobinpark.appcraft.readingbible.domain.model.BibleSourceType
import com.soobinpark.appcraft.readingbible.domain.model.BibleVerse
import com.soobinpark.appcraft.readingbible.domain.model.BibleVersion
import com.soobinpark.appcraft.readingbible.domain.repository.BibleRepository
import java.io.File

class FileBibleRepository(
    private val scanner: BibleFileScanner = BibleFileScanner(),
    private val bdfParser: BdfBibleFileParser = BdfBibleFileParser(),
    private val lfaParser: LfaBibleFileParser = LfaBibleFileParser(),
    private val safScanner: SafBibleFileScanner? = null,
    private val safBdfParser: SafBdfBibleFileParser? = null,
    private val safLfaParser: SafLfaBibleFileParser? = null,
) : BibleRepository {
    constructor(context: Context) : this(
        safScanner = SafBibleFileScanner(context),
        safBdfParser = SafBdfBibleFileParser(context),
        safLfaParser = SafLfaBibleFileParser(context),
    )

    override suspend fun scanVersions(root: File): List<BibleVersion> = scanner.scan(root)

    suspend fun scanVersions(treeUri: Uri): List<BibleVersion> = safScanner?.scan(treeUri).orEmpty()

    override suspend fun readChapter(
        version: BibleVersion,
        book: BibleBook,
        chapter: Int,
    ): List<BibleVerse> = if (version.treeUri != null) {
        when (version.sourceType) {
            BibleSourceType.BdfSplit -> safBdfParser?.readChapter(version, book, chapter).orEmpty()
            BibleSourceType.LfaArchive -> safLfaParser?.readChapter(version, book, chapter).orEmpty()
        }
    } else {
        when (version.sourceType) {
            BibleSourceType.BdfSplit -> bdfParser.readChapter(version, book, chapter)
            BibleSourceType.LfaArchive -> lfaParser.readChapter(version, book, chapter)
        }
    }
}
