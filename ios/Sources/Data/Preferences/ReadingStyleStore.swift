import Foundation

final class ReadingStyleStore: @unchecked Sendable {
    private let defaults = UserDefaults.standard
    private let key = "reading_style"

    func load() -> ReadingStyle {
        guard let data = defaults.data(forKey: key),
              let style = try? JSONDecoder().decode(ReadingStyle.self, from: data)
        else { return ReadingStyle() }
        return style
    }

    func save(_ style: ReadingStyle) {
        var normalized = style
        normalized.fontSize = min(max(style.fontSize, ReadingStyle.fontSizeRange.lowerBound), ReadingStyle.fontSizeRange.upperBound)
        normalized.lineHeightMultiplier = min(
            max(style.lineHeightMultiplier, ReadingStyle.lineHeightRange.lowerBound),
            ReadingStyle.lineHeightRange.upperBound
        )
        if let data = try? JSONEncoder().encode(normalized) {
            defaults.set(data, forKey: key)
        }
    }
}
