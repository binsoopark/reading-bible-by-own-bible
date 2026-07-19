import Foundation

@MainActor
final class AppEnvironment: ObservableObject {
    let dataFolderStore = DataFolderStore()
    let bookmarkStore = BookmarkStore()
    let progressStore = ReadingProgressStore()
    let styleStore = ReadingStyleStore()
    let personalNoteStore = PersonalNoteStore()

    private(set) var repository: FileBibleRepository!

    @Published var dataFolderURL: URL?
    @Published var localDownloadRoot: URL?
    @Published var readingStyle: ReadingStyle
    @Published var bookmarks: [VerseBookmark] = []
    @Published var personalNotes: [PersonalNote] = []
    @Published var cacheWarmUpMessage: String?
    @Published var cacheWarmUpProgress: Double?
    @Published var dataDownloadState = BibleDataDownloadState()
    @Published private(set) var isBootstrapped = false
    @Published private(set) var bootstrapError: String?

    init() {
        BibleStoragePaths.ensurePersistentDirectories()
        readingStyle = ReadingStyleStore().load()
        dataFolderURL = dataFolderStore.resolvedFolderURL()
        localDownloadRoot = dataFolderStore.getLocalDownloadRoot()
        Task { await bootstrap() }
    }

    func bootstrap() async {
        defer { isBootstrapped = true }
        do {
            let cache = try BibleChapterCache()
            repository = FileBibleRepository(cache: cache)

            if let resolved = dataFolderStore.resolvedFolderURL() {
                dataFolderURL = resolved
                if dataFolderStore.getLocalDownloadRoot()?.path == resolved.path {
                    localDownloadRoot = resolved
                }
            } else {
                dataFolderURL = nil
                localDownloadRoot = dataFolderStore.getLocalDownloadRoot()
                if localDownloadRoot != nil {
                    dataFolderURL = localDownloadRoot
                }
            }

            dataDownloadState.installedRoot = localDownloadRoot ?? dataFolderURL
            bookmarks = bookmarkStore.load()
            personalNotes = personalNoteStore.load()
            readingStyle = styleStore.load()
            await warmUpIfNeeded()
        } catch {
            bootstrapError = error.localizedDescription
            cacheWarmUpMessage = "초기화 실패: \(error.localizedDescription)"
        }
    }

    func setDataFolder(_ url: URL) {
        do {
            try dataFolderStore.saveFolderBookmark(url)
            dataFolderURL = url
            Task { await warmUpIfNeeded() }
        } catch {
            dataFolderStore.setFallbackPath(url)
            dataFolderURL = url
        }
    }

    func updateStyle(_ style: ReadingStyle) {
        readingStyle = style
        styleStore.save(style)
    }

    func toggleBookmark(for verse: BibleVerse) {
        if var existing = bookmarks.first(where: { $0.key == verse.id }) {
            existing.isBookmarked.toggle()
            existing.createdAtMillis = Int64(Date().timeIntervalSince1970 * 1000)
            upsert(existing)
        } else {
            let bookmark = VerseBookmark(
                versionCode: verse.versionCode,
                bookIndex: verse.bookIndex,
                chapter: verse.chapter,
                verse: verse.verse,
                text: verse.text,
                note: "",
                isBookmarked: true,
                highlight: .none,
                isRead: false,
                createdAtMillis: Int64(Date().timeIntervalSince1970 * 1000)
            )
            bookmarks.insert(bookmark, at: 0)
        }
        bookmarkStore.save(bookmarks)
    }

    func toggleHighlight(for verse: BibleVerse, color: VerseHighlight = .yellow) {
        if var existing = bookmarks.first(where: { $0.key == verse.id }) {
            existing.highlight = existing.highlight == .none ? color : .none
            existing.createdAtMillis = Int64(Date().timeIntervalSince1970 * 1000)
            upsert(existing)
        } else {
            let bookmark = VerseBookmark(
                versionCode: verse.versionCode,
                bookIndex: verse.bookIndex,
                chapter: verse.chapter,
                verse: verse.verse,
                text: verse.text,
                note: "",
                isBookmarked: false,
                highlight: color,
                isRead: false,
                createdAtMillis: Int64(Date().timeIntervalSince1970 * 1000)
            )
            bookmarks.insert(bookmark, at: 0)
        }
        bookmarkStore.save(bookmarks)
    }

    func setHighlight(for verses: [BibleVerse], color: VerseHighlight) {
        guard !verses.isEmpty else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        for verse in verses {
            if var existing = bookmarks.first(where: { $0.key == verse.id }) {
                existing.highlight = color
                existing.createdAtMillis = now
                upsert(existing)
            } else if color != .none {
                upsert(VerseBookmark(
                    versionCode: verse.versionCode,
                    bookIndex: verse.bookIndex,
                    chapter: verse.chapter,
                    verse: verse.verse,
                    text: verse.text,
                    note: "",
                    isBookmarked: false,
                    highlight: color,
                    isRead: false,
                    createdAtMillis: now
                ))
            }
        }
        bookmarkStore.save(bookmarks)
    }

    func toggleRead(for verse: BibleVerse) {
        if var existing = bookmarks.first(where: { $0.key == verse.id }) {
            existing.isRead.toggle()
            existing.createdAtMillis = Int64(Date().timeIntervalSince1970 * 1000)
            upsert(existing)
        } else {
            let bookmark = VerseBookmark(
                versionCode: verse.versionCode,
                bookIndex: verse.bookIndex,
                chapter: verse.chapter,
                verse: verse.verse,
                text: verse.text,
                note: "",
                isBookmarked: false,
                highlight: .none,
                isRead: true,
                createdAtMillis: Int64(Date().timeIntervalSince1970 * 1000)
            )
            bookmarks.insert(bookmark, at: 0)
        }
        bookmarkStore.save(bookmarks)
    }

    func updateNote(for bookmark: VerseBookmark, note: String) {
        updateVerseNote(
            versionCode: bookmark.versionCode,
            bookIndex: bookmark.bookIndex,
            chapter: bookmark.chapter,
            verse: bookmark.verse,
            text: bookmark.text,
            note: note
        )
    }

    func updateVerseNote(for verse: BibleVerse, note: String) {
        updateVerseNote(
            versionCode: verse.versionCode,
            bookIndex: verse.bookIndex,
            chapter: verse.chapter,
            verse: verse.verse,
            text: verse.text,
            note: note
        )
    }

    private func updateVerseNote(
        versionCode: String,
        bookIndex: Int,
        chapter: Int,
        verse: Int,
        text: String,
        note: String
    ) {
        let key = BibleVerse.key(versionCode: versionCode, bookIndex: bookIndex, chapter: chapter, verse: verse)
        if let existing = bookmarks.first(where: { $0.key == key }) {
            let updated = VerseBookmark(
                versionCode: existing.versionCode,
                bookIndex: existing.bookIndex,
                chapter: existing.chapter,
                verse: existing.verse,
                text: text,
                note: note,
                isBookmarked: existing.isBookmarked,
                highlight: existing.highlight,
                isRead: existing.isRead,
                createdAtMillis: Int64(Date().timeIntervalSince1970 * 1000)
            )
            upsert(updated)
        } else if !note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let bookmark = VerseBookmark(
                versionCode: versionCode,
                bookIndex: bookIndex,
                chapter: chapter,
                verse: verse,
                text: text,
                note: note,
                isBookmarked: false,
                highlight: .none,
                isRead: false,
                createdAtMillis: Int64(Date().timeIntervalSince1970 * 1000)
            )
            upsert(bookmark)
        }
        bookmarkStore.save(bookmarks)
    }

    func addPersonalNote(title: String = "", body: String = "") -> PersonalNote {
        let note = PersonalNote.create(title: title, body: body)
        personalNotes.insert(note, at: 0)
        personalNoteStore.save(personalNotes)
        return note
    }

    func updatePersonalNote(_ note: PersonalNote) {
        guard let index = personalNotes.firstIndex(where: { $0.id == note.id }) else { return }
        var updated = note
        updated.updatedAtMillis = Int64(Date().timeIntervalSince1970 * 1000)
        personalNotes[index] = updated
        personalNotes.sort { $0.updatedAtMillis > $1.updatedAtMillis }
        personalNoteStore.save(personalNotes)
    }

    func deletePersonalNote(id: String) {
        personalNotes.removeAll { $0.id == id }
        personalNoteStore.save(personalNotes)
    }

    func exportPersonalNotes(ids: Set<String>) throws -> Data {
        let selected = personalNotes.filter { ids.contains($0.id) }
        return try PersonalNoteBackupCodec.encode(selected)
    }

    func importPersonalNotes(from data: Data, merge: Bool = true) throws {
        let imported = try PersonalNoteBackupCodec.decode(data)
        if merge {
            var merged = Dictionary(uniqueKeysWithValues: personalNotes.map { ($0.id, $0) })
            for note in imported {
                merged[note.id] = note
            }
            personalNotes = merged.values.sorted { $0.updatedAtMillis > $1.updatedAtMillis }
        } else {
            personalNotes = imported.sorted { $0.updatedAtMillis > $1.updatedAtMillis }
        }
        personalNoteStore.save(personalNotes)
    }

    func bookmark(for verse: BibleVerse) -> VerseBookmark? {
        bookmarks.first { $0.key == verse.id }
    }

    func exportRecordsJSON() -> String {
        bookmarkStore.exportJSON()
    }

    func importRecordsJSON(_ raw: String) {
        bookmarks = bookmarkStore.importJSON(raw)
    }

    func downloadSampleBibleData() async {
        guard let repository else { return }
        if dataDownloadState.isActive { return }

        if await BibleDataDownloader.hasExistingData(
            repository: repository,
            dataFolderURL: dataFolderURL,
            localDownloadRoot: localDownloadRoot ?? dataFolderStore.getLocalDownloadRoot()
        ) {
            dataDownloadState = BibleDataDownloadState(
                message: "이미 성경 데이터가 있습니다. 다운로드는 데이터가 없을 때만 이용할 수 있습니다.",
                installedRoot: localDownloadRoot ?? dataFolderURL
            )
            return
        }

        do {
            let root = try await BibleDataDownloader.downloadAndInstall(repository: repository) { state in
                Task { @MainActor in
                    self.dataDownloadState = state
                }
            }
            dataFolderStore.setLocalDownloadRoot(root)
            dataFolderURL = root
            localDownloadRoot = root
            await warmUpIfNeeded()
        } catch {
            dataDownloadState = BibleDataDownloadState(
                message: "성경 데이터 다운로드에 실패했습니다. \(error.localizedDescription)",
                installedRoot: localDownloadRoot
            )
        }
    }

    var hasExistingBibleDataConfigured: Bool {
        dataFolderURL != nil || localDownloadRoot != nil
    }

    private func upsert(_ bookmark: VerseBookmark) {
        if bookmark.shouldPersist {
            replace(bookmark)
        } else {
            bookmarks.removeAll { $0.key == bookmark.key }
        }
    }

    private func replace(_ bookmark: VerseBookmark) {
        if let index = bookmarks.firstIndex(where: { $0.key == bookmark.key }) {
            bookmarks[index] = bookmark
        } else {
            bookmarks.insert(bookmark, at: 0)
        }
        bookmarks.sort { $0.createdAtMillis > $1.createdAtMillis }
    }

    private func warmUpIfNeeded() async {
        guard let root = dataFolderURL, let repository else { return }
        let scoped = dataFolderStore.startAccessing() ?? root
        defer { dataFolderStore.stopAccessing(scoped) }
        do {
            let versions = try await repository.scanVersions(at: scoped)
            guard let version = versions.first else { return }
            cacheWarmUpMessage = "캐시 준비 중..."
            try await repository.warmUpVersion(version) { current, total, label in
                Task { @MainActor in
                    self.cacheWarmUpProgress = Double(current) / Double(total)
                    self.cacheWarmUpMessage = label
                }
            }
            cacheWarmUpMessage = nil
            cacheWarmUpProgress = nil
        } catch {
            cacheWarmUpMessage = nil
        }
    }
}
