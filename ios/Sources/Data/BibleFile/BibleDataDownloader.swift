import CryptoKit
import Foundation
import ZIPFoundation

struct BibleDataDownloadState: Equatable {
    var isActive = false
    var isError = false
    var message = ""
    var progress: Double?
    var installedRoot: URL?
}

enum BibleDataDownloader {
    static let zipURL = URL(string: "https://github.com/binsoopark/reading-bible-by-own-bible/releases/download/bible-data-2026.05.31/bible.zip")!
    static let expectedSha256 = "071576aeac0514b4010ddc71e86cafd9e24ab3a47876415cf359d5ccd8c4ddc4"

    static func hasExistingData(
        repository: BibleRepository,
        dataFolderURL: URL?,
        localDownloadRoot: URL?
    ) async -> Bool {
        if let localDownloadRoot,
           BibleStoragePaths.containsBibleFiles(at: localDownloadRoot),
           (try? await repository.scanVersions(at: localDownloadRoot).isEmpty) == false {
            return true
        }
        if let discovered = BibleStoragePaths.discoverPersistedDownloadRoot(),
           (try? await repository.scanVersions(at: discovered).isEmpty) == false {
            return true
        }
        if let dataFolderURL,
           (try? await repository.scanVersions(at: dataFolderURL).isEmpty) == false {
            return true
        }
        return false
    }

    static func downloadAndInstall(
        repository: BibleRepository,
        onProgress: @Sendable (BibleDataDownloadState) -> Void
    ) async throws -> URL {
        let fileManager = FileManager.default
        BibleStoragePaths.ensurePersistentDirectories()
        let installBase = BibleStoragePaths.downloadBase
        let stageDir = BibleStoragePaths.downloadStage
        let cacheFile = fileManager.temporaryDirectory.appendingPathComponent("bible.zip")

        onProgress(BibleDataDownloadState(isActive: true, message: L10n.text("download.progress.downloading"), progress: 0))

        let (tempURL, _) = try await URLSession.shared.download(from: zipURL)
        try? fileManager.removeItem(at: cacheFile)
        try fileManager.moveItem(at: tempURL, to: cacheFile)

        onProgress(BibleDataDownloadState(isActive: true, message: L10n.text("download.progress.verifying"), progress: 0.72))
        let hash = try sha256(of: cacheFile)
        guard hash == expectedSha256 else {
            throw NSError(domain: "BibleDataDownloader", code: 1, userInfo: [
                NSLocalizedDescriptionKey: L10n.text("download.error.checksum"),
            ])
        }

        onProgress(BibleDataDownloadState(isActive: true, message: L10n.text("download.progress.extracting"), progress: 0.82))
        try? fileManager.removeItem(at: stageDir)
        try fileManager.createDirectory(at: stageDir, withIntermediateDirectories: true)
        try fileManager.unzipItem(at: cacheFile, to: stageDir)

        try? fileManager.removeItem(at: installBase)
        try fileManager.moveItem(at: stageDir, to: installBase)

        let root = installBase.appendingPathComponent(BibleStoragePaths.downloadFolderName, isDirectory: true)
        let finalRoot = fileManager.fileExists(atPath: root.path) ? root : installBase
        _ = try await repository.scanVersions(at: finalRoot)

        try? fileManager.removeItem(at: cacheFile)
        onProgress(BibleDataDownloadState(isActive: false, message: L10n.text("download.success"), progress: 1, installedRoot: finalRoot))
        return finalRoot
    }

    private static func sha256(of url: URL) throws -> String {
        let data = try Data(contentsOf: url)
        let digest = SHA256.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
