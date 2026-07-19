import Foundation

enum BibleVersionNames {
    static func displayName(for code: String) -> String {
        let normalized = code.lowercased()
        if let name = names[normalized] { return name }
        let stripped = normalized
            .replacingOccurrences(of: "kor", with: "")
            .replacingOccurrences(of: "eng", with: "")
            .uppercased()
        return stripped.isEmpty ? code : stripped
    }

    private static let names: [String: String] = [
        "kornkrv_hs": "개역개정",
        "korktv": "바른성경",
        "korhkjv": "KJV 흠정역",
        "korkkjv": "KJV 흠정역",
        "korhrv": "개역한글",
        "kornkrv": "개역개정",
        "kornrsv": "새번역",
        "kornkcb": "공동번역 개정판",
        "koreasy": "쉬운성경",
        "kordob": "우리말성경",
        "korklb": "현대인의 성경",
        "korhchv": "개역한글 국한문 병행",
        "korcath": "가톨릭성경",
        "kortkv": "현대어성경",
        "engniv": "NIV",
        "engkjv": "KJV",
        "engnasb": "NASB",
        "engnlt": "NLT",
        "enggnt": "GNT",
        "engmsg": "MSG",
        "engnkjv": "NKJV",
        "engnrsv": "NRSV",
        "engtniv": "TNIV",
        "grestg": "헬라어 구약 Septuagint",
        "grestp": "헬라어 신약 Stephanos",
        "latvul": "Latin Vulgate",
        "hebrewwlc": "Hebrew WLC",
        "hebrewbhs": "Hebrew BHS",
        "hebmod": "Modern Hebrew",
        "spnrei": "Spanish Reina Valera",
        "hymns": "찬송가",
        "new_hymns": "새찬송가",
        "versicles": "교독문",
        "new_versicle": "새교독문",
    ]
}
