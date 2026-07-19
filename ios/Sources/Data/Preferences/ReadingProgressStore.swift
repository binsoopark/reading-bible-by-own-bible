import Foundation

final class ReadingProgressStore: @unchecked Sendable {
    private let defaults = UserDefaults.standard

    func load() -> ReadingProgress {
        ReadingProgress(
            versionCode: defaults.string(forKey: "last_version_code"),
            bookIndex: defaults.integer(forKey: "last_book_index"),
            chapter: max(defaults.integer(forKey: "last_chapter"), 1)
        )
    }

    func save(_ progress: ReadingProgress) {
        defaults.set(progress.versionCode, forKey: "last_version_code")
        defaults.set(progress.bookIndex, forKey: "last_book_index")
        defaults.set(progress.chapter, forKey: "last_chapter")
    }
}
