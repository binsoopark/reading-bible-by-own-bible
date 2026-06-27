import Foundation

@MainActor
final class SearchViewModel: ObservableObject {
    @Published var query = ""
    @Published var selectedVersion: BibleVersion?
    @Published var results: [BibleSearchResult] = []
    @Published var isSearching = false
    @Published var progressLabel = ""
    @Published var progressValue: Double = 0
    @Published var resultPage = 0

    private var repository: BibleRepository?
    private var dataFolderStore: DataFolderStore?
    private var readerVersionCode: String?

    let pageSize = 100

    func configure(environment: AppEnvironment, readerVersionCode: String?) {
        repository = environment.repository
        dataFolderStore = environment.dataFolderStore
        self.readerVersionCode = readerVersionCode
    }

    func syncVersions(from versions: [BibleVersion]) {
        if selectedVersion == nil {
            selectedVersion = versions.first { $0.code.caseInsensitiveCompare(readerVersionCode ?? "") == .orderedSame }
                ?? versions.first
        }
    }

    func syncReaderVersionCode(_ code: String?) {
        readerVersionCode = code
    }

    func search(environment: AppEnvironment) async {
        configure(environment: environment, readerVersionCode: readerVersionCode)
        guard let repository, let dataFolderStore else { return }
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2, let version = selectedVersion else {
            results = []
            return
        }
        guard let root = environment.dataFolderURL ?? dataFolderStore.defaultBibleFolder() else { return }

        isSearching = true
        resultPage = 0
        let scoped = dataFolderStore.startAccessing() ?? root
        defer { dataFolderStore.stopAccessing(scoped) }

        do {
            let useCase = SearchBibleUseCase(repository: repository)
            results = try await useCase(version: version.withRoot(scoped), query: trimmed) { current, total, label in
                Task { @MainActor in
                    self.progressValue = Double(current) / Double(total)
                    self.progressLabel = label
                }
            }
        } catch {
            results = []
        }
        isSearching = false
    }

    var pagedResults: [BibleSearchResult] {
        let end = min((resultPage + 1) * pageSize, results.count)
        return Array(results.prefix(end))
    }

    var canLoadMore: Bool { pagedResults.count < results.count }
}
