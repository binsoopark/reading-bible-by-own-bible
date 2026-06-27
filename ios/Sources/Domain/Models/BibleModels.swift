import Foundation

enum BibleSourceType: String, Codable, Sendable {
    case bdfSplit
    case lfaArchive
}

struct BibleVersion: Identifiable, Hashable, Sendable {
    var id: String { "\(code)-\(sourceType.rawValue)" }
    let code: String
    let displayName: String
    let sourceType: BibleSourceType
    let fileRoot: URL

    func withRoot(_ root: URL) -> BibleVersion {
        BibleVersion(code: code, displayName: displayName, sourceType: sourceType, fileRoot: root)
    }
}

struct BibleBook: Identifiable, Hashable, Sendable {
    var id: Int { index }
    let index: Int
    let koreanName: String
    let englishName: String
    let chapterCount: Int
}

struct BibleVerse: Identifiable, Hashable, Sendable {
    var id: String { "\(versionCode):\(bookIndex):\(chapter):\(verse)" }
    let versionCode: String
    let bookIndex: Int
    let chapter: Int
    let verse: Int
    let text: String

    static func key(versionCode: String, bookIndex: Int, chapter: Int, verse: Int) -> String {
        [versionCode, String(bookIndex), String(chapter), String(verse)].joined(separator: ":")
    }
}

struct ReadingSelection: Sendable {
    let version: BibleVersion?
    let book: BibleBook
    let chapter: Int
}

struct BibleSearchResult: Identifiable, Sendable {
    var id: String { verse.id }
    let verse: BibleVerse
    let book: BibleBook
    let snippet: String
}
