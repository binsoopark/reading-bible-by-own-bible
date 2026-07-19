import Foundation

final class PersonalNoteStore: @unchecked Sendable {
    private let defaults = UserDefaults.standard
    private let key = "personal_notes"

    func load() -> [PersonalNote] {
        guard let data = defaults.data(forKey: key),
              let notes = try? JSONDecoder().decode([PersonalNote].self, from: data)
        else { return [] }
        return notes.sorted { $0.updatedAtMillis > $1.updatedAtMillis }
    }

    func save(_ notes: [PersonalNote]) {
        let sorted = notes.sorted { $0.updatedAtMillis > $1.updatedAtMillis }
        if let data = try? JSONEncoder().encode(sorted) {
            defaults.set(data, forKey: key)
        }
    }
}
