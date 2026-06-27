package com.soobinpark.appcraft.readingbible.data.biblefile

object BibleVersionNames {
    fun displayNameFor(code: String): String {
        val normalized = code.lowercase()
        return names[normalized] ?: normalized
            .removePrefix("kor")
            .removePrefix("eng")
            .uppercase()
            .takeIf { it.isNotBlank() }
            ?: code
    }

    private val names = mapOf(
        "kornkrv_hs" to "개역개정",
        "korktv" to "바른성경",
        "korhkjv" to "KJV 흠정역",
        "korkkjv" to "KJV 흠정역",
        "korhrv" to "개역한글",
        "kornkrv" to "개역개정",
        "kornrsv" to "새번역",
        "kornkcb" to "공동번역 개정판",
        "koreasy" to "쉬운성경",
        "kordob" to "우리말성경",
        "korklb" to "현대인의 성경",
        "kchhrv" to "개역한글 국한문",
        "korhchv" to "개역한글 국한문 병행",
        "kchnkrv" to "개역개정 국한문",
        "kchktv" to "바른성경 국한문",
        "korcath" to "가톨릭성경",
        "kortkv" to "현대어성경",
        "engniv" to "NIV",
        "engkjv" to "KJV",
        "engnasb" to "NASB",
        "engnlt" to "NLT",
        "engesv" to "ESV",
        "enggnt" to "GNT",
        "enghcsb" to "HCSB",
        "engmsg" to "MSG",
        "engisv" to "ISV",
        "engnkjv" to "NKJV",
        "engnrsv" to "NRSV",
        "engtniv" to "TNIV",
        "engdrb" to "Darby",
        "engylt" to "YLT",
        "engasv" to "ASV",
        "engwmt" to "Weymouth",
        "hbrtrl" to "원어 음역",
        "grestg" to "헬라어 구약 Septuagint",
        "grestp" to "헬라어 신약 Stephanos",
        "latvul" to "Latin Vulgate",
        "hebrewwlc" to "Hebrew WLC",
        "hebrewbhs" to "Hebrew BHS",
        "hebmod" to "Modern Hebrew",
        "spnrei" to "Spanish Reina Valera",
        "hymns" to "찬송가",
        "new_hymns" to "새찬송가",
        "versicles" to "교독문",
        "new_versicle" to "새교독문",
    )
}
