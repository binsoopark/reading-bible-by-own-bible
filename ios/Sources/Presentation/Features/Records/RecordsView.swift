import SwiftUI

struct RecordsView: View {
    @EnvironmentObject private var environment: AppEnvironment
    @Environment(\.readingPalette) private var palette
    @State private var filter: RecordFilter = .bookmark
    @State private var selectedKeys: Set<String> = []
    @State private var showShareSheet = false
    @State private var sharePayload = ""
    @State private var copiedNotice = false

    private var filteredBookmarks: [VerseBookmark] {
        environment.bookmarks.filter { $0.matches(filter: filter) }
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
                Text("선택한 구절을 복사했습니다.")
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
            Text("메모")
                .font(.title3.weight(.semibold))
                .foregroundStyle(palette.onSurface)
            Picker("기록 종류", selection: $filter) {
                ForEach(RecordFilter.allCases) { item in
                    Text(item.label).tag(item)
                }
            }
            .pickerStyle(.segmented)
            Text(filterDescription)
                .font(.caption)
                .readingSecondaryForeground()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 8)
    }

    private var filterDescription: String {
        switch filter {
        case .bookmark: "북마크한 구절만 표시합니다."
        case .highlight: "형광펜으로 표시한 구절만 표시합니다."
        case .read: "읽음으로 체크한 구절만 표시합니다."
        case .note: "메모가 있는 구절만 표시합니다."
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
                Text(emptyTitle)
                    .font(.headline)
                    .foregroundStyle(palette.onSurface)
                Text(emptyMessage)
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
        case .bookmark: "북마크 없음"
        case .highlight: "형광펜 표시 없음"
        case .read: "읽음 표시 없음"
        case .note: "메모 없음"
        }
    }

    private var emptyMessage: String {
        switch filter {
        case .bookmark: "읽기 탭에서 북마크 아이콘을 누르면 여기에 표시됩니다."
        case .highlight: "읽기 탭에서 형광펜 아이콘을 눌러 표시하면 여기에 나타납니다."
        case .read: "읽기 탭에서 읽음 체크를 하면 여기에 표시됩니다."
        case .note: "기록 카드에서 메모를 작성하면 여기에 표시됩니다."
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
                TextField("메모", text: binding(for: bookmark), axis: .vertical)
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
            if selectedKeys.isEmpty { return }
            toggleSelection(bookmark.key)
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
