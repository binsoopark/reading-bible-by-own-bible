import Foundation

struct BibleFileScanner: Sendable {
    func scan(at root: URL) throws -> [BibleVersion] {
        let fileManager = FileManager.default
        guard fileManager.fileExists(atPath: root.path) else { return [] }

        let files = try fileManager.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)
        let lfaVersions = files
            .filter { $0.pathExtension.lowercased() == "lfa" }
            .map { url in
                let code = url.deletingPathExtension().lastPathComponent
                return BibleVersion(
                    code: code,
                    displayName: BibleVersionNames.displayName(for: code),
                    sourceType: .lfaArchive,
                    fileRoot: root
                )
            }

        let bdfFiles = files.filter { $0.pathExtension.lowercased() == "bdf" }
        let bdfPrefixes = Set(
            bdfFiles.compactMap { url -> String? in
                let name = url.deletingPathExtension().lastPathComponent
                let prefix = stripTrailingDigits(from: name)
                return prefix.isEmpty ? nil : prefix
            }
        )

        let bdfVersions = bdfPrefixes.compactMap { code -> BibleVersion? in
            let complete = (1 ... 7).allSatisfy { index in
                fileManager.fileExists(atPath: root.appendingPathComponent("\(code)\(index).bdf").path)
            }
            guard complete else { return nil }
            return BibleVersion(
                code: code,
                displayName: BibleVersionNames.displayName(for: code),
                sourceType: .bdfSplit,
                fileRoot: root
            )
        }

        var seen = Set<String>()
        return (lfaVersions + bdfVersions)
            .filter { seen.insert($0.code.lowercased()).inserted }
            .sorted { $0.code.localizedCaseInsensitiveCompare($1.code) == .orderedAscending }
    }

    private func stripTrailingDigits(from name: String) -> String {
        var value = name
        while value.last?.isNumber == true {
            value.removeLast()
        }
        return value
    }
}
