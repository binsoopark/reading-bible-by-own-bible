import Foundation

enum VerseLineParser {
    static func parse(
        versionCode: String,
        bookIndex: Int,
        chapter: Int,
        bookNumber: String,
        chapterNeedle: String,
        line: String
    ) -> BibleVerse? {
        guard line.hasPrefix(bookNumber) else { return nil }
        guard line.contains(chapterNeedle) else { return nil }
        guard let colonIndex = line.firstIndex(of: ":") else { return nil }
        guard let needleRange = line.range(of: chapterNeedle) else { return nil }
        let colonOffset = line.distance(from: line.startIndex, to: colonIndex)
        let needleOffset = line.distance(from: line.startIndex, to: needleRange.lowerBound)
        if needleOffset >= colonOffset + 1 { return nil }

        let afterColon = String(line[line.index(after: colonIndex)...]).trimmingCharacters(in: .whitespaces)
        let parts = afterColon.split(separator: " ", maxSplits: 1, omittingEmptySubsequences: true)
        guard let verseNumber = Int(parts.first ?? ""), verseNumber > 0 else { return nil }
        let text = parts.count > 1 ? String(parts[1]).trimmingCharacters(in: .whitespaces) : ""
        guard !text.isEmpty else { return nil }
        return BibleVerse(
            versionCode: versionCode,
            bookIndex: bookIndex,
            chapter: chapter,
            verse: verseNumber,
            text: text
        )
    }
}

enum BibleTextEncoding {
    static func decodeLines(from url: URL) throws -> [String] {
        let data = try Data(contentsOf: url)
        for encoding in [String.Encoding.ms949, .utf8, .utf16, .isoLatin1] {
            if let text = String(data: data, encoding: encoding) {
                return text.components(separatedBy: .newlines)
            }
        }
        return String(decoding: data, as: UTF8.self).components(separatedBy: .newlines)
    }

    static func decodeLfaText(_ data: Data) -> String {
        let utf8 = String(decoding: data, as: UTF8.self)
        let replacementCount = utf8.filter { $0 == "\u{FFFD}" }.count
        if replacementCount > 3, let ms949 = String(data: data, encoding: .ms949) {
            return ms949
        }
        return utf8
    }
}

extension String.Encoding {
    static let ms949 = String.Encoding(
        rawValue: CFStringConvertEncodingToNSStringEncoding(
            CFStringEncoding(CFStringEncodings.dosKorean.rawValue)
        )
    )
}
