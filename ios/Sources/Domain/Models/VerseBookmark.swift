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
        case .none: "없음"
        case .yellow: "노랑"
        case .mint: "민트"
        case .blue: "파랑"
        case .pink: "분홍"
        case .lavender: "라벤더"
        case .orange: "주황"
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
