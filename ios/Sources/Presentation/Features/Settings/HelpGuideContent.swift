import Foundation

enum HelpGuideContent {
    static var intro: String { L10n.text("help.intro") }

    static var sections: [HelpSection] {
        [
            HelpSection("help.section.tabs", [
                HelpItem("tab.reader", "help.tab.reader.detail"), HelpItem("tab.search", "help.tab.search.detail"),
                HelpItem("tab.records", "help.tab.records.detail"), HelpItem("tab.personalNotes", "help.tab.personalNotes.detail"),
                HelpItem("tab.settings", "help.tab.settings.detail"),
            ]),
            HelpSection("help.section.readerBasic", [
                HelpItem("help.bookChapter.title", "help.bookChapter.detail"), HelpItem("help.prevNext.title", "help.prevNext.detail"),
                HelpItem("help.versionCompare.title", "help.versionCompare.detail"),
            ]),
            HelpSection("help.section.readerGestures", [
                HelpItem("help.swipe.title", "help.swipe.detail"), HelpItem("help.pinch.title", "help.pinch.detail"),
                HelpItem("help.keyboard.title", "help.keyboard.detail"),
            ]),
            HelpSection("help.section.readerActions", [
                HelpItem("record.filter.bookmark", "help.bookmark.detail"), HelpItem("help.rangeHighlight.title", "help.rangeHighlight.detail"),
                HelpItem("help.readCheck.title", "help.readCheck.detail"), HelpItem("help.verseNote.title", "help.verseNote.detail"),
                HelpItem("help.noteIcon.title", "help.noteIcon.detail"),
            ]),
            HelpSection("help.section.readerSelection", [
                HelpItem("help.selectionStart.title", "help.selectionStart.detail"), HelpItem("help.selectionToggle.title", "help.selectionToggle.detail"),
                HelpItem("help.copyShare.title", "help.copyShare.readerDetail"),
            ]),
            HelpSection("help.section.search", [
                HelpItem("help.search.title", "help.search.detail"), HelpItem("help.moreResults.title", "help.moreResults.detail"),
                HelpItem("help.openReader.title", "help.openReader.detail"),
            ]),
            HelpSection("help.section.records", [
                HelpItem("help.recordTypes.title", "help.recordTypes.detail"), HelpItem("help.highlightColor.title", "help.highlightColor.detail"),
                HelpItem("help.verseNote.title", "help.verseNote.recordsDetail"), HelpItem("help.copyShare.title", "help.copyShare.recordsDetail"),
            ]),
            HelpSection("help.section.personalNotes", [
                HelpItem("help.addNote.title", "help.addNote.detail"), HelpItem("help.editDelete.title", "help.editDelete.detail"),
                HelpItem("action.backup", "help.backup.detail"), HelpItem("action.restore", "help.restore.detail"),
            ]),
            HelpSection("help.section.settings", [
                HelpItem("help.bibleData.title", "help.bibleData.detail"), HelpItem("help.readingStyle.title", "help.readingStyle.detail"),
                HelpItem("help.recordBackup.title", "help.recordBackup.detail"),
            ]),
        ]
    }
}

struct HelpSection: Identifiable {
    let id: String
    let title: String
    let items: [HelpItem]

    init(_ titleKey: String, _ items: [HelpItem]) {
        id = titleKey
        title = L10n.text(titleKey)
        self.items = items
    }
}

struct HelpItem: Identifiable {
    let id: String
    let title: String
    let detail: String

    init(_ titleKey: String, _ detailKey: String) {
        id = "\(titleKey).\(detailKey)"
        title = L10n.text(titleKey)
        detail = L10n.text(detailKey)
    }
}
