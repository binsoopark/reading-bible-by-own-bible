import Foundation
import ZIPFoundation

struct LfaBibleFileParser: Sendable {
    func readChapter(version: BibleVersion, book: BibleBook, chapter: Int) throws -> [BibleVerse] {
        let archiveURL = version.fileRoot.appendingPathComponent("\(version.code).lfa")
        guard FileManager.default.fileExists(atPath: archiveURL.path) else { return [] }

        let bookNumber = BibleCatalog.bookNumber(forBookIndex: book.index)
        let chapterNeedle = " \(chapter):"
        let entryName = "\(version.code)\(bookNumber)_\(chapter).lfb"

        guard let archive = Archive(url: archiveURL, accessMode: .read),
              let entry = archive.first(where: {
                  $0.path.caseInsensitiveCompare(entryName) == .orderedSame
              })
        else { return [] }

        var data = Data()
        _ = try archive.extract(entry) { chunk in data.append(chunk) }
        let text = BibleTextEncoding.decodeLfaText(data)

        return text.components(separatedBy: .newlines).compactMap { line in
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
