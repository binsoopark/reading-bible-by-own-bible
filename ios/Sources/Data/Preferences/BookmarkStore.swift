import Foundation

final class BookmarkStore: @unchecked Sendable {
    private let defaults = UserDefaults.standard
    private let key = "verse_bookmarks"

    func load() -> [VerseBookmark] {
        guard let raw = defaults.string(forKey: key),
              let data = raw.data(using: .utf8),
              let bookmarks = try? JSONDecoder().decode([VerseBookmark].self, from: data)
        else { return [] }
        return bookmarks
            .filter(\.shouldPersist)
            .sorted { $0.createdAtMillis > $1.createdAtMillis }
    }

    func save(_ bookmarks: [VerseBookmark]) {
        let unique = Dictionary(grouping: bookmarks, by: \.key)
            .compactMap { $0.value.max(by: { $0.createdAtMillis < $1.createdAtMillis }) }
            .sorted { $0.createdAtMillis > $1.createdAtMillis }
        if let data = try? JSONEncoder().encode(unique),
           let raw = String(data: data, encoding: .utf8) {
            defaults.set(raw, forKey: key)
        }
    }

    func exportJSON() -> String {
        guard let data = try? JSONEncoder().encode(load()),
              let raw = String(data: data, encoding: .utf8)
        else { return "[]" }
        return raw
    }

    func importJSON(_ raw: String) -> [VerseBookmark] {
        guard let data = raw.data(using: .utf8),
              let imported = try? JSONDecoder().decode([VerseBookmark].self, from: data)
        else { return load() }
        let merged = Dictionary(
            grouping: imported + load(),
            by: \.key
        ).compactMap { $0.value.max(by: { $0.createdAtMillis < $1.createdAtMillis }) }
        save(merged)
        return merged
    }
}
