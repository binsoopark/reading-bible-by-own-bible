import Foundation

enum BibleFileIssueSeverity: Sendable {
    case info
    case warning
    case error
}

struct BibleFileIssue: Identifiable, Sendable {
    let id = UUID()
    let title: String
    let detail: String
    let severity: BibleFileIssueSeverity
}

struct BibleFileDiagnostic: Sendable {
    let rootLabel: String
    let scannedFileCount: Int
    let lfaCount: Int
    let bdfFileCount: Int
    let completeBdfVersionCount: Int
    let mp3Count: Int
    let unknownFileCount: Int
    let versions: [BibleVersion]
    let issues: [BibleFileIssue]
}
