import Foundation

struct BibleFileDiagnosticsReader: Sendable {
    private let scanner = BibleFileScanner()

    func diagnose(at root: URL) throws -> BibleFileDiagnostic {
        let fileManager = FileManager.default
        let files = try fileManager.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)
        let versions = try scanner.scan(at: root)
        var issues: [BibleFileIssue] = []

        let bdfFiles = files.filter { $0.pathExtension.lowercased() == "bdf" }
        let lfaFiles = files.filter { $0.pathExtension.lowercased() == "lfa" }
        let mp3Files = files.filter { $0.pathExtension.lowercased() == "mp3" }
        let knownExtensions = Set(["bdf", "lfa", "lfb", "mp3"])
        let unknownCount = files.filter { !knownExtensions.contains($0.pathExtension.lowercased()) }.count

        let incompleteBdf = Dictionary(grouping: bdfFiles) { url -> String in
            stripTrailingDigits(from: url.deletingPathExtension().lastPathComponent)
        }.filter { prefix, group in
            let indices = Set(group.compactMap { url -> Int? in
                let suffix = url.deletingPathExtension().lastPathComponent.drop(while: { !$0.isNumber })
                return Int(suffix)
            })
            return indices.count > 0 && indices.count < 7
        }

        for (prefix, group) in incompleteBdf where !prefix.isEmpty {
            issues.append(BibleFileIssue(
                title: L10n.format("diagnostics.incompleteBdf.title", prefix),
                detail: L10n.format("diagnostics.incompleteBdf.detail", group.count),
                severity: .warning
            ))
        }

        if versions.isEmpty {
            issues.append(BibleFileIssue(
                title: L10n.text("diagnostics.noVersion.title"),
                detail: L10n.text("diagnostics.noVersion.detail"),
                severity: .error
            ))
        }

        let completeBdfCount = versions.filter { $0.sourceType == .bdfSplit }.count

        return BibleFileDiagnostic(
            rootLabel: root.path,
            scannedFileCount: files.count,
            lfaCount: lfaFiles.count,
            bdfFileCount: bdfFiles.count,
            completeBdfVersionCount: completeBdfCount,
            mp3Count: mp3Files.count,
            unknownFileCount: unknownCount,
            versions: versions,
            issues: issues
        )
    }

    private func stripTrailingDigits(from name: String) -> String {
        var value = name
        while value.last?.isNumber == true {
            value.removeLast()
        }
        return value
    }
}
