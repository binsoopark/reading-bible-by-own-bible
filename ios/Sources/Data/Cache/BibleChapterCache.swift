import Foundation
import SQLite3

actor BibleChapterCache {
    private var db: OpaquePointer?
    private let dbURL: URL

    init() throws {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let folder = support.appendingPathComponent("ReadingMyBible", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        dbURL = folder.appendingPathComponent("bible_chapter_cache.db")
        try openDatabase()
        try createTablesIfNeeded()
    }

    deinit {
        if db != nil { sqlite3_close(db) }
    }

    func readChapter(cacheKey: String) throws -> [BibleVerse]? {
        let sql = "SELECT version_code, book_index, chapter, verses_blob FROM chapters WHERE cache_key = ? LIMIT 1"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else { return nil }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, cacheKey, -1, SQLITE_TRANSIENT)
        guard sqlite3_step(statement) == SQLITE_ROW else { return nil }
        let versionCode = String(cString: sqlite3_column_text(statement, 0))
        let bookIndex = Int(sqlite3_column_int(statement, 1))
        let chapter = Int(sqlite3_column_int(statement, 2))
        guard let blobCString = sqlite3_column_text(statement, 3) else { return nil }
        let blob = String(cString: blobCString)
        return decodeVersesBlob(blob, versionCode: versionCode, bookIndex: bookIndex, chapter: chapter)
    }

    func writeChapter(cacheKey: String, versionCode: String, bookIndex: Int, chapter: Int, verses: [BibleVerse]) throws {
        let blob = encodeVersesBlob(verses)
        let searchText = verses.map(\.text).joined(separator: "\n").lowercased()
        let sql = """
        INSERT INTO chapters(cache_key, version_code, book_index, chapter, cached_at, verses_blob, search_text)
        VALUES(?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(cache_key) DO UPDATE SET
            cached_at=excluded.cached_at,
            verses_blob=excluded.verses_blob,
            search_text=excluded.search_text
        """
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else { return }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, cacheKey, -1, SQLITE_TRANSIENT)
        sqlite3_bind_text(statement, 2, versionCode, -1, SQLITE_TRANSIENT)
        sqlite3_bind_int(statement, 3, Int32(bookIndex))
        sqlite3_bind_int(statement, 4, Int32(chapter))
        sqlite3_bind_int64(statement, 5, Int64(Date().timeIntervalSince1970 * 1000))
        sqlite3_bind_text(statement, 6, blob, -1, SQLITE_TRANSIENT)
        sqlite3_bind_text(statement, 7, searchText, -1, SQLITE_TRANSIENT)
        sqlite3_step(statement)
    }

    func searchCachedChapters(prefix: String, query: String) throws -> [(bookIndex: Int, chapter: Int)] {
        let escaped = query
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "%", with: "\\%")
            .replacingOccurrences(of: "_", with: "\\_")
        let pattern = "%\(escaped.lowercased())%"
        let sql = """
        SELECT book_index, chapter FROM chapters
        WHERE cache_key LIKE ? ESCAPE '\\' AND search_text LIKE ? ESCAPE '\\'
        ORDER BY book_index, chapter
        """
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else { return [] }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, "\(prefix)%", -1, SQLITE_TRANSIENT)
        sqlite3_bind_text(statement, 2, pattern, -1, SQLITE_TRANSIENT)
        var results: [(Int, Int)] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            results.append((Int(sqlite3_column_int(statement, 0)), Int(sqlite3_column_int(statement, 1))))
        }
        return results
    }

    func isWarmUpComplete(_ key: String) throws -> Bool {
        let sql = "SELECT 1 FROM warmups WHERE warmup_key = ? LIMIT 1"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else { return false }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, key, -1, SQLITE_TRANSIENT)
        return sqlite3_step(statement) == SQLITE_ROW
    }

    func markWarmUpComplete(_ key: String) throws {
        let sql = "INSERT OR REPLACE INTO warmups(warmup_key, completed_at) VALUES(?, ?)"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else { return }
        defer { sqlite3_finalize(statement) }
        sqlite3_bind_text(statement, 1, key, -1, SQLITE_TRANSIENT)
        sqlite3_bind_int64(statement, 2, Int64(Date().timeIntervalSince1970 * 1000))
        sqlite3_step(statement)
    }

    private func openDatabase() throws {
        if sqlite3_open(dbURL.path, &db) != SQLITE_OK {
            throw NSError(domain: "BibleChapterCache", code: 1)
        }
    }

    private func createTablesIfNeeded() throws {
        let sql = """
        CREATE TABLE IF NOT EXISTS chapters (
            cache_key TEXT PRIMARY KEY,
            version_code TEXT NOT NULL,
            book_index INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            cached_at INTEGER NOT NULL,
            verses_blob TEXT,
            search_text TEXT
        );
        CREATE TABLE IF NOT EXISTS warmups (
            warmup_key TEXT PRIMARY KEY,
            completed_at INTEGER NOT NULL
        );
        """
        if sqlite3_exec(db, sql, nil, nil, nil) != SQLITE_OK {
            throw NSError(domain: "BibleChapterCache", code: 2)
        }
    }

    private func encodeVersesBlob(_ verses: [BibleVerse]) -> String {
        verses.map { verse in
            let encoded = Data(verse.text.utf8).base64EncodedString()
            return "\(verse.verse)\t\(encoded)"
        }.joined(separator: "\n")
    }

    private func decodeVersesBlob(_ blob: String, versionCode: String, bookIndex: Int, chapter: Int) -> [BibleVerse]? {
        let verses = blob.split(separator: "\n", omittingEmptySubsequences: true).compactMap { line -> BibleVerse? in
            let parts = line.split(separator: "\t", maxSplits: 1)
            guard parts.count == 2, let verse = Int(parts[0]),
                  let data = Data(base64Encoded: String(parts[1])),
                  let text = String(data: data, encoding: .utf8)
            else { return nil }
            return BibleVerse(versionCode: versionCode, bookIndex: bookIndex, chapter: chapter, verse: verse, text: text)
        }
        return verses.isEmpty ? nil : verses
    }
}

private let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
