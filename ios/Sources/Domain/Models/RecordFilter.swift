import Foundation

enum RecordFilter: String, CaseIterable, Identifiable, Sendable {
    case bookmark
    case highlight
    case read
    case note

    var id: String { rawValue }

    var label: String {
        switch self {
        case .bookmark: "북마크"
        case .highlight: "형광펜"
        case .read: "읽음"
        case .note: "메모"
        }
    }

    func matches(_ bookmark: VerseBookmark) -> Bool {
        switch self {
        case .bookmark: bookmark.isBookmarked
        case .highlight: bookmark.highlight != .none
        case .read: bookmark.isRead
        case .note: !bookmark.note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }
}

enum RecordShareFormatter {
    static func format(_ bookmarks: [VerseBookmark]) -> String {
        let sorted = bookmarks.sorted {
            if $0.bookIndex != $1.bookIndex { return $0.bookIndex < $1.bookIndex }
            if $0.chapter != $1.chapter { return $0.chapter < $1.chapter }
            return $0.verse < $1.verse
        }
        guard let first = sorted.first else { return "" }

        var sections: [String] = []
        var currentHeader = ""
        var lines: [String] = []

        func flush() {
            guard !lines.isEmpty else { return }
            sections.append([currentHeader, lines.joined(separator: "\n")].filter { !$0.isEmpty }.joined(separator: "\n"))
            lines = []
        }

        for bookmark in sorted {
            let book = BibleCatalog.book(at: bookmark.bookIndex)
            let header = "\(book.koreanName) \(bookmark.chapter)장"
            if header != currentHeader {
                flush()
                currentHeader = header
            }
            lines.append("\(bookmark.verse)절 \(bookmark.text)")
        }
        flush()
        return sections.joined(separator: "\n\n")
    }
}
