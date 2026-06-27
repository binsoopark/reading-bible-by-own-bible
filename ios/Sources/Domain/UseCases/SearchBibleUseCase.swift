import Foundation

struct SearchBibleUseCase: Sendable {
    let repository: BibleRepository

    func callAsFunction(
        version: BibleVersion,
        query: String,
        onProgress: @Sendable (Int, Int, String) -> Void = { _, _, _ in }
    ) async throws -> [BibleSearchResult] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else { return [] }
        return try await repository.search(version: version, query: trimmed, onProgress: onProgress)
    }
}
