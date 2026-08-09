import Foundation

@MainActor
final class ReaderViewModel: ObservableObject {
    @Published var versions: [BibleVersion] = []
    @Published var selectedVersion: BibleVersion?
    @Published var comparisonVersion: BibleVersion?
    @Published var bookIndex = 0
    @Published var chapter = 1
    @Published var focusVerse: Int?
    @Published var verses: [BibleVerse] = []
    @Published var comparisonVerses: [BibleVerse] = []
    @Published var isLoading = true
    @Published var loadingMessage = "성경 데이터를 준비하는 중입니다."
    @Published var message: String?

    private var repository: BibleRepository?
    private var progressStore: ReadingProgressStore?
    private var dataFolderStore: DataFolderStore?
    private var dataRoot: URL?
    private var loadGeneration = 0

    func configure(environment: AppEnvironment) {
        repository = environment.repository
        progressStore = environment.progressStore
        dataFolderStore = environment.dataFolderStore
    }

    func refresh(environment: AppEnvironment) async {
        configure(environment: environment)

        guard environment.isBootstrapped else { return }

        if let bootstrapError = environment.bootstrapError {
            isLoading = false
            message = "앱 초기화에 실패했습니다. \(bootstrapError)"
            return
        }

        guard let repository, let dataFolderStore else {
            isLoading = false
            message = "성경 데이터를 불러올 준비가 되지 않았습니다."
            return
        }

        dataRoot = environment.dataFolderURL ?? dataFolderStore.defaultBibleFolder()
        guard let root = dataRoot else {
            isLoading = false
            message = "성경 데이터 폴더를 선택하거나 샘플 데이터를 다운로드해 주세요."
            versions = []
            selectedVersion = nil
            verses = []
            return
        }

        isLoading = true
        loadingMessage = "데이터 폴더에서 BDF/LFA 역본을 찾는 중입니다."
        message = nil

        let scoped = dataFolderStore.startAccessing() ?? root
        defer { dataFolderStore.stopAccessing(scoped) }

        do {
            let scanned = try await repository.scanVersions(at: scoped)
            versions = scanned
            let saved = progressStore?.load() ?? ReadingProgress()
            bookIndex = min(max(saved.bookIndex, 0), BibleCatalog.books.count - 1)
            let book = BibleCatalog.book(at: bookIndex)
            chapter = min(max(saved.chapter, 1), book.chapterCount)

            selectedVersion = saved.versionCode.flatMap { code in
                scanned.first { $0.code.caseInsensitiveCompare(code) == .orderedSame }
            } ?? scanned.first
            let defaults = UserDefaults.standard
            if defaults.bool(forKey: Self.comparisonVersionSavedKey) {
                comparisonVersion = defaults.string(forKey: Self.comparisonVersionCodeKey).flatMap { code in
                    scanned.first {
                        $0.code.caseInsensitiveCompare(code) == .orderedSame && $0.id != selectedVersion?.id
                    }
                }
            } else {
                comparisonVersion = scanned.first { $0.id != selectedVersion?.id }
            }

            loadingMessage = selectedVersion.map { "\($0.displayName) \(book.koreanName) \(chapter)장을 여는 중입니다." }
                ?? "사용 가능한 역본을 확인하는 중입니다."

            if scanned.isEmpty {
                isLoading = false
                message = "선택한 폴더에서 BDF/LFA 역본을 찾지 못했습니다."
                verses = []
                return
            }

            await reloadCurrentChapter()
            isLoading = false
        } catch {
            isLoading = false
            message = error.localizedDescription
        }
    }

    func selectVersion(_ version: BibleVersion) {
        selectedVersion = versions.first { $0.id == version.id } ?? version
        if comparisonVersion?.id == selectedVersion?.id {
            comparisonVersion = nil
        }
        message = nil
        Task { await reloadCurrentChapter() }
        saveProgress()
    }

    func selectComparison(_ version: BibleVersion?) {
        comparisonVersion = version?.id == selectedVersion?.id ? nil : version
        Task { await reloadCurrentChapter() }
        saveProgress()
    }

    func selectBook(_ index: Int, chapter newChapter: Int = 1) {
        bookIndex = index
        chapter = newChapter
        focusVerse = nil
        Task { await reloadCurrentChapter() }
        saveProgress()
    }

    func moveChapter(by delta: Int) {
        var newBookIndex = bookIndex
        var newChapter = chapter + delta
        while newChapter < 1 {
            if newBookIndex == 0 { return }
            newBookIndex -= 1
            newChapter += BibleCatalog.book(at: newBookIndex).chapterCount
        }
        while newChapter > BibleCatalog.book(at: newBookIndex).chapterCount {
            if newBookIndex >= BibleCatalog.books.count - 1 { return }
            newChapter -= BibleCatalog.book(at: newBookIndex).chapterCount
            newBookIndex += 1
        }
        bookIndex = newBookIndex
        chapter = newChapter
        focusVerse = 1
        message = nil
        Task { await reloadCurrentChapter() }
        saveProgress()
    }

    func openSearchResult(_ verse: BibleVerse) {
        if let version = versions.first(where: { $0.code.caseInsensitiveCompare(verse.versionCode) == .orderedSame }) {
            selectedVersion = version
        }
        bookIndex = verse.bookIndex
        chapter = verse.chapter
        focusVerse = verse.verse
        Task { await reloadCurrentChapter() }
        saveProgress()
    }

    func openBookmark(_ bookmark: VerseBookmark) {
        openSearchResult(BibleVerse(
            versionCode: bookmark.versionCode,
            bookIndex: bookmark.bookIndex,
            chapter: bookmark.chapter,
            verse: bookmark.verse,
            text: bookmark.text
        ))
    }

    var currentBook: BibleBook { BibleCatalog.book(at: bookIndex) }

    private func reloadCurrentChapter() async {
        loadGeneration += 1
        let generation = loadGeneration
        await loadChapter(expectedGeneration: generation)
    }

    private func resolvedDataRoot(using dataFolderStore: DataFolderStore) -> URL? {
        dataRoot ?? dataFolderStore.resolvedFolderURL() ?? dataFolderStore.defaultBibleFolder()
    }

    private func loadChapter(expectedGeneration: Int) async {
        guard expectedGeneration == loadGeneration else { return }
        guard let repository, let dataFolderStore, let selectedVersion else {
            verses = []
            comparisonVerses = []
            return
        }
        guard let root = resolvedDataRoot(using: dataFolderStore) else {
            verses = []
            comparisonVerses = []
            return
        }

        let scoped = dataFolderStore.startAccessing() ?? root
        defer { dataFolderStore.stopAccessing(scoped) }

        let book = currentBook
        let version = selectedVersion.withRoot(scoped)
        let comparison = comparisonVersion?.withRoot(scoped)
        let targetBookIndex = bookIndex
        let targetChapter = chapter

        do {
            let loadedVerses = try await repository.readChapter(version: version, book: book, chapter: targetChapter)
            guard expectedGeneration == loadGeneration else { return }
            guard bookIndex == targetBookIndex, chapter == targetChapter else { return }

            verses = loadedVerses
            if let comparison {
                comparisonVerses = try await repository.readChapter(
                    version: comparison,
                    book: book,
                    chapter: targetChapter
                )
            } else {
                comparisonVerses = []
            }
        } catch {
            guard expectedGeneration == loadGeneration else { return }
            message = error.localizedDescription
        }
    }

    private func saveProgress() {
        progressStore?.save(ReadingProgress(
            versionCode: selectedVersion?.code,
            bookIndex: bookIndex,
            chapter: chapter
        ))
        let defaults = UserDefaults.standard
        defaults.set(comparisonVersion?.code, forKey: Self.comparisonVersionCodeKey)
        defaults.set(true, forKey: Self.comparisonVersionSavedKey)
    }

    private static let comparisonVersionCodeKey = "last_comparison_version_code"
    private static let comparisonVersionSavedKey = "has_saved_comparison_version"
}
