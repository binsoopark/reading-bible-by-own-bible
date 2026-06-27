import Foundation

protocol BibleRepository: Sendable {
    func scanVersions(at root: URL) async throws -> [BibleVersion]
    func readChapter(version: BibleVersion, book: BibleBook, chapter: Int) async throws -> [BibleVerse]
    func warmUpVersion(_ version: BibleVersion, onProgress: @Sendable (Int, Int, String) -> Void) async throws
    func search(version: BibleVersion, query: String, onProgress: @Sendable (Int, Int, String) -> Void) async throws -> [BibleSearchResult]
    func diagnose(at root: URL) async throws -> BibleFileDiagnostic
}
