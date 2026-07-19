import Foundation

final class DataFolderStore: @unchecked Sendable {
    private let defaults = UserDefaults.standard
    private let bookmarkKey = "data_folder_bookmark"
    private let fallbackPathKey = "data_folder_fallback_path"
    private let localDownloadRootKey = "bible_data_local_root"

    init() {
        BibleStoragePaths.ensurePersistentDirectories()
    }

    func saveFolderBookmark(_ url: URL) throws {
        let data = try url.bookmarkData(
            options: .minimalBookmark,
            includingResourceValuesForKeys: nil,
            relativeTo: nil
        )
        defaults.set(data, forKey: bookmarkKey)
        defaults.removeObject(forKey: fallbackPathKey)
        defaults.removeObject(forKey: localDownloadRootKey)
        defaults.removeObject(forKey: BibleStoragePaths.downloadInstalledKey)
    }

    func getLocalDownloadRoot() -> URL? {
        if let discovered = BibleStoragePaths.discoverPersistedDownloadRoot() {
            syncDownloadRoot(discovered)
            return discovered
        }

        guard let path = defaults.string(forKey: localDownloadRootKey) else { return nil }
        let url = URL(fileURLWithPath: path, isDirectory: true)
        guard FileManager.default.fileExists(atPath: url.path),
              BibleStoragePaths.containsBibleFiles(at: url)
        else { return nil }

        syncDownloadRoot(url)
        return url
    }

    func setLocalDownloadRoot(_ url: URL) {
        defaults.set(true, forKey: BibleStoragePaths.downloadInstalledKey)
        defaults.set(url.path, forKey: localDownloadRootKey)
        defaults.removeObject(forKey: bookmarkKey)
        defaults.removeObject(forKey: fallbackPathKey)
    }

    func resolvedFolderURL() -> URL? {
        if let bookmarkURL = resolveBookmarkURL() {
            return bookmarkURL
        }
        if let downloaded = getLocalDownloadRoot() {
            return downloaded
        }
        if let path = defaults.string(forKey: fallbackPathKey) {
            let url = URL(fileURLWithPath: path, isDirectory: true)
            if BibleStoragePaths.containsBibleFiles(at: url) {
                return url
            }
        }
        return defaultBibleFolder()
    }

    func setFallbackPath(_ url: URL) {
        defaults.set(url.path, forKey: fallbackPathKey)
        defaults.removeObject(forKey: bookmarkKey)
        defaults.removeObject(forKey: localDownloadRootKey)
        defaults.removeObject(forKey: BibleStoragePaths.downloadInstalledKey)
    }

    func defaultBibleFolder() -> URL? {
        let candidate = BibleStoragePaths.documentsBibleRoot
        if BibleStoragePaths.containsBibleFiles(at: candidate) {
            return candidate
        }
        return nil
    }

    func hasPersistedBibleData() -> Bool {
        resolvedFolderURL() != nil
    }

    func startAccessing() -> URL? {
        guard let url = resolvedFolderURL() else { return nil }
        _ = url.startAccessingSecurityScopedResource()
        return url
    }

    func stopAccessing(_ url: URL?) {
        url?.stopAccessingSecurityScopedResource()
    }

    private func resolveBookmarkURL() -> URL? {
        guard let data = defaults.data(forKey: bookmarkKey) else { return nil }
        var stale = false
        guard let url = try? URL(
            resolvingBookmarkData: data,
            options: .withoutUI,
            relativeTo: nil,
            bookmarkDataIsStale: &stale
        ) else {
            return nil
        }

        if stale {
            try? saveFolderBookmark(url)
        }
        return url
    }

    private func syncDownloadRoot(_ url: URL) {
        defaults.set(true, forKey: BibleStoragePaths.downloadInstalledKey)
        if defaults.string(forKey: localDownloadRootKey) != url.path {
            defaults.set(url.path, forKey: localDownloadRootKey)
        }
    }
}
