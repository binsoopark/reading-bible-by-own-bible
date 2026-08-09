import Foundation

struct PersonalNote: Identifiable, Codable, Hashable, Sendable {
    let id: String
    var title: String
    var body: String
    var createdAtMillis: Int64
    var updatedAtMillis: Int64

    static func create(title: String = "", body: String = "") -> PersonalNote {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        return PersonalNote(
            id: UUID().uuidString,
            title: title,
            body: body,
            createdAtMillis: now,
            updatedAtMillis: now
        )
    }
}

struct PersonalNoteBackupFile: Codable, Sendable {
    static let formatId = "reading-my-bible-personal-notes"
    static let currentVersion = 1

    let format: String
    let version: Int
    let exportedAtMillis: Int64
    let notes: [PersonalNote]

    init(notes: [PersonalNote]) {
        format = Self.formatId
        version = Self.currentVersion
        exportedAtMillis = Int64(Date().timeIntervalSince1970 * 1000)
        self.notes = notes
    }
}

enum PersonalNoteBackupCodec {
    static func encode(_ notes: [PersonalNote]) throws -> Data {
        let file = PersonalNoteBackupFile(notes: notes)
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try encoder.encode(file)
    }

    static func decode(_ data: Data) throws -> [PersonalNote] {
        let file = try JSONDecoder().decode(PersonalNoteBackupFile.self, from: data)
        guard file.format == PersonalNoteBackupFile.formatId else {
            throw NSError(domain: "PersonalNoteBackup", code: 1, userInfo: [
                NSLocalizedDescriptionKey: L10n.text("personalNotes.invalidBackup"),
            ])
        }
        return file.notes
    }

    static func encodeString(_ notes: [PersonalNote]) throws -> String {
        String(data: try encode(notes), encoding: .utf8) ?? ""
    }
}
