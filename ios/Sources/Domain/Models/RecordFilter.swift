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

enum RecordSortOrder: String, CaseIterable, Identifiable, Sendable {
    case recent
    case bible

    var id: String { rawValue }

    var label: String {
        switch self {
        case .recent: "최신순"
        case .bible: "성경순"
        }
    }
}

enum RecordShareFormatter {
    static func format(_ bookmarks: [VerseBookmark]) -> String {
        let sorted = bookmarks.sorted {
            let versionComparison = $0.versionCode.localizedCaseInsensitiveCompare($1.versionCode)
            if versionComparison != .orderedSame { return versionComparison == .orderedAscending }
            if $0.bookIndex != $1.bookIndex { return $0.bookIndex < $1.bookIndex }
            if $0.chapter != $1.chapter { return $0.chapter < $1.chapter }
            return $0.verse < $1.verse
        }
        guard !sorted.isEmpty else { return "" }

        var sections: [String] = []
        var currentKey: String?
        var sectionBookmarks: [VerseBookmark] = []

        func flush() {
            guard let first = sectionBookmarks.first else { return }
            let book = BibleCatalog.book(at: first.bookIndex)
            let verseLabel = verseRangeLabel(sectionBookmarks.map(\.verse))
            let reference = "\(book.koreanName) \(first.chapter):\(verseLabel) · \(first.versionCode)"
            if sectionBookmarks.count == 1 {
                sections.append("“\(first.text.trimmingCharacters(in: .whitespacesAndNewlines))”\n\(reference)")
            } else {
                let body = sectionBookmarks.map {
                    "\($0.verse)절 \($0.text.trimmingCharacters(in: .whitespacesAndNewlines))"
                }.joined(separator: "\n")
                sections.append("\(body)\n\n\(reference)")
            }
            sectionBookmarks = []
        }

        for bookmark in sorted {
            let key = "\(bookmark.versionCode.lowercased())|\(bookmark.bookIndex)|\(bookmark.chapter)"
            if currentKey != nil, key != currentKey {
                flush()
            }
            currentKey = key
            sectionBookmarks.append(bookmark)
        }
        flush()
        return sections.joined(separator: "\n\n")
    }

    private static func verseRangeLabel(_ verses: [Int]) -> String {
        let sorted = Array(Set(verses)).sorted()
        guard let first = sorted.first else { return "" }
        var ranges: [String] = []
        var start = first
        var end = first
        for verse in sorted.dropFirst() {
            if verse == end + 1 {
                end = verse
            } else {
                ranges.append(start == end ? "\(start)" : "\(start)-\(end)")
                start = verse
                end = verse
            }
        }
        ranges.append(start == end ? "\(start)" : "\(start)-\(end)")
        return ranges.joined(separator: ", ")
    }
}
