import Foundation

enum BibleStoragePaths {
    static let downloadBaseName = "downloaded-bible"
    static let downloadFolderName = "bible"
    static let documentsFolderName = "bible"
    static let downloadInstalledKey = "bible_download_installed"

    static var applicationSupportDirectory: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
    }

    static var documentsDirectory: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
    }

    static var downloadBase: URL {
        applicationSupportDirectory.appendingPathComponent(downloadBaseName, isDirectory: true)
    }

    static var downloadStage: URL {
        applicationSupportDirectory.appendingPathComponent("\(downloadBaseName)-stage", isDirectory: true)
    }

    static var standardDownloadRoot: URL {
        let nested = downloadBase.appendingPathComponent(downloadFolderName, isDirectory: true)
        if FileManager.default.fileExists(atPath: nested.path) {
            return nested
        }
        return downloadBase
    }

    static var documentsBibleRoot: URL {
        documentsDirectory.appendingPathComponent(documentsFolderName, isDirectory: true)
    }

    static func ensurePersistentDirectories() {
        let fileManager = FileManager.default
        for url in [applicationSupportDirectory, downloadBase, documentsDirectory] {
            try? fileManager.createDirectory(at: url, withIntermediateDirectories: true)
        }
    }

    static func containsBibleFiles(at root: URL) -> Bool {
        guard FileManager.default.fileExists(atPath: root.path),
              let files = try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)
        else { return false }

        return files.contains { url in
            let ext = url.pathExtension.lowercased()
            return ext == "bdf" || ext == "lfa"
        }
    }

    static func discoverPersistedDownloadRoot() -> URL? {
        let candidates = [standardDownloadRoot, downloadBase, documentsBibleRoot]
        for candidate in candidates where containsBibleFiles(at: candidate) {
            return candidate
        }
        return nil
    }
}
