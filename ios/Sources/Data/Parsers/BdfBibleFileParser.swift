import Foundation

struct BdfBibleFileParser: Sendable {
    func readChapter(version: BibleVersion, book: BibleBook, chapter: Int) throws -> [BibleVerse] {
        let fileIndex = BibleCatalog.fileIndex(forBookIndex: book.index)
        let fileURL = version.fileRoot.appendingPathComponent("\(version.code)\(fileIndex).bdf")
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return [] }

        let bookNumber = BibleCatalog.bookNumber(forBookIndex: book.index)
        let chapterNeedle = " \(chapter):"
        let lines = try BibleTextEncoding.decodeLines(from: fileURL)

        return lines.compactMap { line in
            VerseLineParser.parse(
                versionCode: version.code,
                bookIndex: book.index,
                chapter: chapter,
                bookNumber: bookNumber,
                chapterNeedle: chapterNeedle,
                line: line
            )
        }
    }
}
