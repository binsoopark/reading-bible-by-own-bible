import Foundation

enum RecordFilter: String, CaseIterable, Identifiable, Sendable {
    case bookmark
    case highlight
    case read
    case note

    var id: String { rawValue }

    var label: String {
        switch self {
        case .bookmark: L10n.text("record.filter.bookmark")
        case .highlight: L10n.text("record.filter.highlight")
        case .read: L10n.text("record.filter.read")
        case .note: L10n.text("record.filter.note")
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
        case .recent: L10n.text("record.sort.recent")
        case .bible: L10n.text("record.sort.bible")
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
            let reference = "\(book.localizedName) \(first.chapter):\(verseLabel) · \(first.versionCode)"
            if sectionBookmarks.count == 1 {
                sections.append("“\(first.text.trimmingCharacters(in: .whitespacesAndNewlines))”\n\(reference)")
            } else {
                let body = sectionBookmarks.map {
                    L10n.format("format.verseLine", $0.verse, $0.text.trimmingCharacters(in: .whitespacesAndNewlines))
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
