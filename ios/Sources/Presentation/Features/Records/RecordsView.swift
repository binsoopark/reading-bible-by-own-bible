import SwiftUI

struct RecordsView: View {
    let onOpenBookmark: (VerseBookmark) -> Void
    @EnvironmentObject private var environment: AppEnvironment
    @Environment(\.readingPalette) private var palette
    @State private var filter: RecordFilter = .bookmark
    @State private var query = ""
    @State private var sortOrder: RecordSortOrder = .recent
    @State private var selectedKeys: Set<String> = []
    @State private var showShareSheet = false
    @State private var sharePayload = ""
    @State private var copiedNotice = false

    private var filteredBookmarks: [VerseBookmark] {
        let normalizedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let filtered = environment.bookmarks.filter { bookmark in
            guard bookmark.matches(filter: filter) else { return false }
            guard !normalizedQuery.isEmpty else { return true }
            let book = BibleCatalog.book(at: bookmark.bookIndex)
            let searchableValues = [
                bookmark.text,
                bookmark.note,
                bookmark.versionCode,
                book.koreanName,
                book.englishName,
                "\(book.koreanName) \(bookmark.chapter):\(bookmark.verse)",
                "\(bookmark.chapter):\(bookmark.verse)",
            ]
            return searchableValues.contains { $0.lowercased().contains(normalizedQuery) }
        }
        switch sortOrder {
        case .recent:
            return filtered.sorted { $0.createdAtMillis > $1.createdAtMillis }
        case .bible:
            return filtered.sorted {
                if $0.bookIndex != $1.bookIndex { return $0.bookIndex < $1.bookIndex }
                if $0.chapter != $1.chapter { return $0.chapter < $1.chapter }
                if $0.verse != $1.verse { return $0.verse < $1.verse }
                return $0.versionCode.localizedCaseInsensitiveCompare($1.versionCode) == .orderedAscending
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            if !selectedKeys.isEmpty {
                SelectionActionBar(
                    count: selectedKeys.count,
                    onClear: { selectedKeys.removeAll() },
                    onCopy: copySelection,
                    onShare: {
                        sharePayload = selectionText
                        showShareSheet = true
                        selectedKeys.removeAll()
                    }
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
            content
        }
        .background(palette.background)
        .onChange(of: filter) { _, _ in
            selectedKeys.removeAll()
        }
        .sheet(isPresented: $showShareSheet) {
            ShareTextSheet(text: sharePayload)
                .presentationDetents([.medium, .large])
        }
        .overlay(alignment: .top) {
            if copiedNotice {
                Text(L10n.text("records.copied"))
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(palette.surfaceContainer, in: Capsule())
                    .padding(.top, 8)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(L10n.text("tab.records"))
                .font(.title3.weight(.semibold))
                .foregroundStyle(palette.onSurface)
            Picker(L10n.text("records.type"), selection: $filter) {
                ForEach(RecordFilter.allCases) { item in
                    Text(item.label).tag(item)
                }
            }
            .pickerStyle(.segmented)
            Text(filterDescription)
                .font(.caption)
                .readingSecondaryForeground()
            TextField(L10n.text("records.searchPlaceholder"), text: $query)
                .textFieldStyle(ReadingTextFieldStyle())
                .onChange(of: query) { _, _ in selectedKeys.removeAll() }
            Picker(L10n.text("records.sort"), selection: $sortOrder) {
                ForEach(RecordSortOrder.allCases) { item in
                    Text(item.label).tag(item)
                }
            }
            .pickerStyle(.segmented)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 8)
    }

    private var filterDescription: String {
        switch filter {
        case .bookmark: L10n.text("records.description.bookmark")
        case .highlight: L10n.text("records.description.highlight")
        case .read: L10n.text("records.description.read")
        case .note: L10n.text("records.description.note")
        }
    }

    @ViewBuilder
    private var content: some View {
        if filteredBookmarks.isEmpty {
            VStack(spacing: 12) {
                Spacer()
                Image(systemName: emptyIcon)
                    .font(.system(size: 40))
                    .foregroundStyle(palette.onSurfaceVariant)
                Text(query.isEmpty ? emptyTitle : L10n.text("records.noSearchResults"))
                    .font(.headline)
                    .foregroundStyle(palette.onSurface)
                Text(query.isEmpty ? emptyMessage : L10n.text("records.tryAnotherQuery"))
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .readingSecondaryForeground()
                    .padding(.horizontal, 32)
                Spacer()
            }
        } else {
            List(filteredBookmarks) { bookmark in
                recordRow(for: bookmark)
            }
            .listStyle(.plain)
            .readingThemedList()
            .dismissKeyboardOnScroll()
            .contentMargins(.bottom, 8, for: .scrollContent)
        }
    }

    private var emptyIcon: String {
        switch filter {
        case .bookmark: "bookmark"
        case .highlight: "highlighter"
        case .read: "checkmark.circle"
        case .note: "note.text"
        }
    }

    private var emptyTitle: String {
        switch filter {
        case .bookmark: L10n.text("records.empty.bookmark.title")
        case .highlight: L10n.text("records.empty.highlight.title")
        case .read: L10n.text("records.empty.read.title")
        case .note: L10n.text("records.empty.note.title")
        }
    }

    private var emptyMessage: String {
        switch filter {
        case .bookmark: L10n.text("records.empty.bookmark.message")
        case .highlight: L10n.text("records.empty.highlight.message")
        case .read: L10n.text("records.empty.read.message")
        case .note: L10n.text("records.empty.note.message")
        }
    }

    @ViewBuilder
    private func recordRow(for bookmark: VerseBookmark) -> some View {
        let isSelected = selectedKeys.contains(bookmark.key)
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("\(BibleCatalog.book(at: bookmark.bookIndex).koreanName) \(bookmark.chapter):\(bookmark.verse) · \(bookmark.versionCode)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(palette.primary)
                Spacer()
                if bookmark.highlight != .none {
                    HighlightColorSwatch(highlight: bookmark.highlight)
                }
            }
            Text(bookmark.text)
                .font(.body)
                .foregroundStyle(palette.onSurface)
            if filter == .note || !bookmark.note.isEmpty {
                TextField(L10n.text("record.filter.note"), text: binding(for: bookmark), axis: .vertical)
                    .textFieldStyle(ReadingTextFieldStyle())
            }
        }
        .padding(.vertical, 4)
        .padding(.horizontal, isSelected ? 8 : 0)
        .background(
            isSelected ? palette.primary.opacity(0.12) : Color.clear,
            in: RoundedRectangle(cornerRadius: 12)
        )
        .overlay {
            if isSelected {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(palette.primary, lineWidth: 2)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            if selectedKeys.isEmpty {
                onOpenBookmark(bookmark)
            } else {
                toggleSelection(bookmark.key)
            }
        }
        .onLongPressGesture {
            toggleSelection(bookmark.key)
        }
    }

    private var selectedBookmarks: [VerseBookmark] {
        filteredBookmarks.filter { selectedKeys.contains($0.key) }
    }

    private var selectionText: String {
        RecordShareFormatter.format(selectedBookmarks)
    }

    private func copySelection() {
        let text = selectionText
        guard !text.isEmpty else { return }
        RecordClipboard.copy(text)
        selectedKeys.removeAll()
        withAnimation {
            copiedNotice = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            withAnimation {
                copiedNotice = false
            }
        }
    }

    private func toggleSelection(_ key: String) {
        if selectedKeys.contains(key) {
            selectedKeys.remove(key)
        } else {
            selectedKeys.insert(key)
        }
    }

    private func binding(for bookmark: VerseBookmark) -> Binding<String> {
        Binding(
            get: { environment.bookmarks.first { $0.key == bookmark.key }?.note ?? "" },
            set: { newValue in
                var updated = bookmark
                updated.note = newValue
                environment.updateNote(for: updated, note: newValue)
            }
        )
    }
}
