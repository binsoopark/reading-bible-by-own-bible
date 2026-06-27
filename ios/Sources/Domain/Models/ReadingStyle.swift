import Foundation

enum ReadingPalette: String, CaseIterable, Codable, Sendable {
    case paper
    case evening
    case oled
    case highContrast
    case warmLight

    var label: String {
        switch self {
        case .paper: "종이"
        case .evening: "저녁"
        case .oled: "OLED"
        case .highContrast: "고대비"
        case .warmLight: "따뜻한 빛"
        }
    }
}

struct ReadingStyle: Codable, Sendable, Equatable {
    var fontSize: Double = 18
    var lineHeightMultiplier: Double = 1.55
    var palette: ReadingPalette = .paper
    var keepScreenOn: Bool = false
    var multitouchZoomEnabled: Bool = true
    var boldTextEnabled: Bool = false
    var showNotesInReader: Bool = true

    static let fontSizeRange = 12.0 ... 28.0
    static let lineHeightRange = 1.2 ... 2.2
}

struct ReadingProgress: Codable, Sendable, Equatable {
    var versionCode: String?
    var bookIndex: Int = 0
    var chapter: Int = 1
}
