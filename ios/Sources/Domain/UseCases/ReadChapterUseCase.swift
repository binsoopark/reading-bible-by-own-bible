import Foundation

struct ReadChapterUseCase: Sendable {
    let repository: BibleRepository

    func callAsFunction(version: BibleVersion, book: BibleBook, chapter: Int) async throws -> [BibleVerse] {
        try await repository.readChapter(version: version, book: book, chapter: chapter)
    }
}
