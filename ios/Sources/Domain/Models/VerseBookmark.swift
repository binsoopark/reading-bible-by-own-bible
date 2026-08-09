import Foundation

enum VerseHighlight: String, CaseIterable, Codable, Sendable {
    case none
    case yellow
    case mint
    case blue
    case pink
    case lavender
    case orange

    var label: String {
        switch self {
        case .none: L10n.text("highlight.none")
        case .yellow: L10n.text("highlight.yellow")
        case .mint: L10n.text("highlight.mint")
        case .blue: L10n.text("highlight.blue")
        case .pink: L10n.text("highlight.pink")
        case .lavender: L10n.text("highlight.lavender")
        case .orange: L10n.text("highlight.orange")
        }
    }
}

struct VerseBookmark: Identifiable, Codable, Hashable, Sendable {
    var id: String { key }
    let versionCode: String
    let bookIndex: Int
    let chapter: Int
    let verse: Int
    let text: String
    var note: String
    var isBookmarked: Bool
    var highlight: VerseHighlight
    var isRead: Bool
    var createdAtMillis: Int64

    var key: String {
        BibleVerse.key(versionCode: versionCode, bookIndex: bookIndex, chapter: chapter, verse: verse)
    }

    var shouldPersist: Bool {
        isBookmarked || !note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || highlight != .none || isRead
    }

    func matches(filter: RecordFilter) -> Bool {
        filter.matches(self)
    }
}
