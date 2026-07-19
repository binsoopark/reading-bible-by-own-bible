import Foundation

final class FileBibleRepository: BibleRepository, @unchecked Sendable {
    private let scanner = BibleFileScanner()
    private let bdfParser = BdfBibleFileParser()
    private let lfaParser = LfaBibleFileParser()
    private let diagnosticsReader = BibleFileDiagnosticsReader()
    private let cache: BibleChapterCache

    private var versionCache: [String: [BibleVersion]] = [:]
    private var chapterMemoryCache: [String: [BibleVerse]] = [:]
    private var warmUpInProgress: Set<String> = []

    init(cache: BibleChapterCache) {
        self.cache = cache
    }

    func scanVersions(at root: URL) async throws -> [BibleVersion] {
        let key = scanKey(for: root)
        if let cached = versionCache[key] { return cached }
        let versions = try scanner.scan(at: root)
        versionCache[key] = versions
        return versions
    }

    func readChapter(version: BibleVersion, book: BibleBook, chapter: Int) async throws -> [BibleVerse] {
        let key = chapterCacheKey(version: version, book: book, chapter: chapter)
        if let cached = chapterMemoryCache[key] { return cached }
        if let persisted = try await cache.readChapter(cacheKey: key) {
            chapterMemoryCache[key] = persisted
            return persisted
        }
        let verses: [BibleVerse]
        switch version.sourceType {
        case .bdfSplit:
            verses = try bdfParser.readChapter(version: version, book: book, chapter: chapter)
        case .lfaArchive:
            verses = try lfaParser.readChapter(version: version, book: book, chapter: chapter)
        }
        if !verses.isEmpty {
            chapterMemoryCache[key] = verses
            try? await cache.writeChapter(
                cacheKey: key,
                versionCode: version.code,
                bookIndex: book.index,
                chapter: chapter,
                verses: verses
            )
        }
        return verses
    }

    func warmUpVersion(
        _ version: BibleVersion,
        onProgress: @Sendable (Int, Int, String) -> Void
    ) async throws {
        let warmKey = warmUpKey(for: version)
        if try await cache.isWarmUpComplete(warmKey) { return }
        guard warmUpInProgress.insert(warmKey).inserted else { return }
        defer { warmUpInProgress.remove(warmKey) }

        let total = BibleCatalog.totalChapters
        var current = 0
        for book in BibleCatalog.books {
            for chapter in 1 ... book.chapterCount {
                current += 1
                if current == 1 || current == total || chapter == 1 || current % 25 == 0 {
                    onProgress(current, total, "\(book.koreanName) \(chapter)장")
                }
                _ = try await readChapter(version: version, book: book, chapter: chapter)
            }
        }
        try await cache.markWarmUpComplete(warmKey)
    }

    func search(
        version: BibleVersion,
        query: String,
        onProgress: @Sendable (Int, Int, String) -> Void
    ) async throws -> [BibleSearchResult] {
        let needle = query.lowercased()
        let prefix = chapterCachePrefix(version: version)
        let cachedRefs = try await cache.searchCachedChapters(prefix: prefix, query: needle)
        var results: [BibleSearchResult] = []
        let total = BibleCatalog.totalChapters
        var current = 0

        for book in BibleCatalog.books {
            for chapter in 1 ... book.chapterCount {
                current += 1
                if current == 1 || current == total || chapter == 1 || current % 25 == 0 {
                    onProgress(current, total, "\(book.koreanName) \(chapter)장")
                }
                let verses: [BibleVerse]
                if cachedRefs.contains(where: { $0.bookIndex == book.index && $0.chapter == chapter }) {
                    verses = try await readChapter(version: version, book: book, chapter: chapter)
                } else {
                    verses = try await readChapter(version: version, book: book, chapter: chapter)
                }
                for verse in verses where verse.text.lowercased().contains(needle) {
                    results.append(BibleSearchResult(verse: verse, book: book, snippet: verse.text))
                }
            }
        }
        return results.sorted {
            if $0.verse.bookIndex != $1.verse.bookIndex { return $0.verse.bookIndex < $1.verse.bookIndex }
            if $0.verse.chapter != $1.verse.chapter { return $0.verse.chapter < $1.verse.chapter }
            return $0.verse.verse < $1.verse.verse
        }
    }

    func diagnose(at root: URL) async throws -> BibleFileDiagnostic {
        try diagnosticsReader.diagnose(at: root)
    }

    private func scanKey(for root: URL) -> String {
        let stamp = (try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey]))?
            .filter { ["bdf", "lfa"].contains($0.pathExtension.lowercased()) }
            .sorted { $0.lastPathComponent.lowercased() < $1.lastPathComponent.lowercased() }
            .map { url -> String in
                let values = try? url.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey])
                let modified = values?.contentModificationDate?.timeIntervalSince1970 ?? 0
                let size = values?.fileSize ?? 0
                return "\(url.lastPathComponent):\(modified):\(size)"
            }
            .joined(separator: "|") ?? ""
        return "file:\(root.path):\(stamp)"
    }

    private func chapterCacheKey(version: BibleVersion, book: BibleBook, chapter: Int) -> String {
        [
            version.fileRoot.path,
            version.code,
            version.sourceType.rawValue,
            sourceStamp(version: version, book: book),
            String(book.index),
            String(chapter),
        ].joined(separator: ":")
    }

    private func chapterCachePrefix(version: BibleVersion) -> String {
        [version.fileRoot.path, version.code, version.sourceType.rawValue].joined(separator: ":") + ":"
    }

    private func warmUpKey(for version: BibleVersion) -> String {
        [version.fileRoot.path, version.code, version.sourceType.rawValue, versionSourceStamp(version: version), "full"].joined(separator: ":")
    }

    private func sourceStamp(version: BibleVersion, book: BibleBook) -> String {
        switch version.sourceType {
        case .bdfSplit:
            let index = BibleCatalog.fileIndex(forBookIndex: book.index)
            return fileStamp(url: version.fileRoot.appendingPathComponent("\(version.code)\(index).bdf"))
        case .lfaArchive:
            return fileStamp(url: version.fileRoot.appendingPathComponent("\(version.code).lfa"))
        }
    }

    private func versionSourceStamp(version: BibleVersion) -> String {
        switch version.sourceType {
        case .bdfSplit:
            return (1 ... 7).map { index in
                fileStamp(url: version.fileRoot.appendingPathComponent("\(version.code)\(index).bdf"))
            }.joined(separator: "|")
        case .lfaArchive:
            return fileStamp(url: version.fileRoot.appendingPathComponent("\(version.code).lfa"))
        }
    }

    private func fileStamp(url: URL) -> String {
        let values = try? url.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey])
        let modified = values?.contentModificationDate?.timeIntervalSince1970 ?? 0
        let size = values?.fileSize ?? 0
        return "\(url.lastPathComponent):\(modified):\(size)"
    }
}
