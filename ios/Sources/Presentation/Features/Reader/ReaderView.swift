import SwiftUI

struct ReaderView: View {
    @EnvironmentObject private var environment: AppEnvironment
    @ObservedObject var viewModel: ReaderViewModel
    @State private var showBookPicker = false
    @State private var showVersionPicker = false
    @State private var showComparisonPicker = false
    @State private var pinchAnchorFontSize: Double?
    @State private var fontSizeHint: Int?
    @State private var swipeOffset: CGFloat = 0
    @State private var selectedVerseKeys: Set<String> = []
    @State private var selectionAnchorKey: String?
    @State private var selectedHighlightColor: VerseHighlight = .yellow
    @State private var showHighlightColorPicker = false
    @State private var applyHighlightToSelectionOnPick = false
    @State private var showShareSheet = false
    @State private var sharePayload = ""
    @State private var copiedNotice = false
    @State private var noteEditingVerse: BibleVerse?
    @State private var noteDraft = ""
    @Environment(\.readingPalette) private var palette

    var body: some View {
        VStack(spacing: 0) {
            readerToolbar
            if !selectedVerseKeys.isEmpty {
                SelectionActionBar(
                    count: selectedVerseKeys.count,
                    onClear: { clearVerseSelection() },
                    onHighlight: {
                        applyHighlightToSelectionOnPick = true
                        showHighlightColorPicker = true
                    },
                    onCopy: copySelectedVerses,
                    onShare: {
                        sharePayload = selectedVersesShareText
                        showShareSheet = true
                        clearVerseSelection()
                    }
                )
                .padding(.horizontal, 12)
                .padding(.bottom, 8)
            }
            if viewModel.isLoading {
                Spacer()
                ProgressView(viewModel.loadingMessage)
                Spacer()
            } else if let message = viewModel.message {
                Spacer()
                ContentUnavailableView("데이터 없음", systemImage: "folder", description: Text(message))
                Spacer()
            } else {
                ZStack {
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: verseSpacing) {
                                ForEach(viewModel.verses) { verse in
                                    VerseCardView(
                                        verse: verse,
                                        comparisonText: comparisonText(for: verse.verse),
                                        fontSize: environment.readingStyle.fontSize,
                                        lineHeight: environment.readingStyle.lineHeightMultiplier,
                                        bold: environment.readingStyle.boldTextEnabled,
                                        bookmark: environment.bookmark(for: verse),
                                        showNoteIcon: environment.readingStyle.showNotesInReader,
                                        isSelected: selectedVerseKeys.contains(verse.id),
                                        isSelectionActive: !selectedVerseKeys.isEmpty,
                                        onBookmark: { environment.toggleBookmark(for: verse) },
                                        onHighlight: {
                                            environment.toggleHighlight(for: verse, color: selectedHighlightColor)
                                        },
                                        onHighlightLongPress: {
                                            applyHighlightToSelectionOnPick = false
                                            showHighlightColorPicker = true
                                        },
                                        onRead: { environment.toggleRead(for: verse) },
                                        onNote: { openNoteEditor(for: verse) },
                                        onSelectToggle: { selectVerseRange(to: verse.id) }
                                    )
                                }
                            }
                            .id(chapterContentID)
                            .padding(.horizontal)
                            .padding(.bottom, 16)
                            .offset(x: swipeOffset * 0.15)
                        }
                        .simultaneousGesture(chapterSwipeGesture)
                        .simultaneousGesture(pinchGesture)
                        .dismissKeyboardOnScroll()
                        .onChange(of: viewModel.focusVerse) { _, verse in
                            guard let verse,
                                  let target = viewModel.verses.first(where: { $0.verse == verse })?.id
                            else { return }
                            withAnimation { proxy.scrollTo(target, anchor: .top) }
                        }
                        .onChange(of: chapterContentID) { _, _ in
                            clearVerseSelection()
                            let target = viewModel.focusVerse.flatMap { focusVerse in
                                viewModel.verses.first { $0.verse == focusVerse }?.id
                            } ?? viewModel.verses.first?.id
                            guard let target else { return }
                            withAnimation { proxy.scrollTo(target, anchor: .top) }
                        }
                    }

                    if let hint = fontSizeHint {
                        Text("\(hint)")
                            .font(.title3.weight(.semibold))
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(.ultraThinMaterial, in: Capsule())
                            .transition(.opacity)
                    }
                }
            }
        }
        .task(id: readerRefreshKey) {
            await viewModel.refresh(environment: environment)
        }
        .sheet(isPresented: $showBookPicker) {
            BookChapterPicker(bookIndex: $viewModel.bookIndex, chapter: $viewModel.chapter) {
                viewModel.selectBook(viewModel.bookIndex, chapter: viewModel.chapter)
            }
        }
        .sheet(isPresented: $showVersionPicker) {
            VersionPicker(title: "역본 선택", versions: viewModel.versions, selection: $viewModel.selectedVersion, allowNone: false)
                .readingTheme(environment.readingStyle.palette)
                .onDisappear {
                    if let version = viewModel.selectedVersion {
                        viewModel.selectVersion(version)
                    }
                }
        }
        .sheet(isPresented: $showComparisonPicker) {
            VersionPicker(title: "비교 역본", versions: viewModel.versions, selection: $viewModel.comparisonVersion, allowNone: true)
                .readingTheme(environment.readingStyle.palette)
                .onDisappear { viewModel.selectComparison(viewModel.comparisonVersion) }
        }
        .sheet(isPresented: $showHighlightColorPicker) {
            HighlightColorPickerSheet(selection: $selectedHighlightColor) { color in
                if applyHighlightToSelectionOnPick {
                    environment.setHighlight(for: selectedVerses, color: color)
                    clearVerseSelection()
                }
                applyHighlightToSelectionOnPick = false
            }
                .readingTheme(environment.readingStyle.palette)
                .onDisappear { applyHighlightToSelectionOnPick = false }
        }
        .sheet(isPresented: $showShareSheet) {
            ShareTextSheet(text: sharePayload)
                .presentationDetents([.medium, .large])
        }
        .sheet(item: $noteEditingVerse) { verse in
            VerseNoteEditorSheet(
                reference: "\(viewModel.currentBook.koreanName) \(verse.chapter):\(verse.verse) · \(verse.versionCode)",
                verseText: verse.text,
                note: $noteDraft
            ) {
                environment.updateVerseNote(for: verse, note: noteDraft)
            }
            .readingTheme(environment.readingStyle.palette)
        }
        .overlay(alignment: .top) {
            if copiedNotice {
                Text("선택한 구절을 복사했습니다.")
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(palette.surfaceContainer, in: Capsule())
                    .padding(.top, 8)
            }
        }
    }

    private var selectedVerses: [BibleVerse] {
        viewModel.verses.filter { selectedVerseKeys.contains($0.id) }
    }

    private var selectedVersesShareText: String {
        RecordShareFormatter.format(
            selectedVerses.map { verse in
                VerseBookmark(
                    versionCode: verse.versionCode,
                    bookIndex: verse.bookIndex,
                    chapter: verse.chapter,
                    verse: verse.verse,
                    text: verse.text,
                    note: "",
                    isBookmarked: false,
                    highlight: .none,
                    isRead: false,
                    createdAtMillis: 0
                )
            }
        )
    }

    private func selectVerseRange(to targetKey: String) {
        if selectedVerseKeys.isEmpty {
            selectedVerseKeys = [targetKey]
            selectionAnchorKey = targetKey
            return
        }
        let anchorKey = selectionAnchorKey.flatMap { key in
            viewModel.verses.contains(where: { $0.id == key }) ? key : nil
        } ?? selectedVerseKeys.first ?? targetKey
        if anchorKey == targetKey, selectedVerseKeys.count == 1 {
            clearVerseSelection()
            return
        }
        guard let anchorIndex = viewModel.verses.firstIndex(where: { $0.id == anchorKey }),
              let targetIndex = viewModel.verses.firstIndex(where: { $0.id == targetKey })
        else {
            selectedVerseKeys = [targetKey]
            selectionAnchorKey = targetKey
            return
        }
        let bounds = min(anchorIndex, targetIndex) ... max(anchorIndex, targetIndex)
        selectedVerseKeys = Set(bounds.map { viewModel.verses[$0].id })
        selectionAnchorKey = anchorKey
    }

    private func clearVerseSelection() {
        selectedVerseKeys.removeAll()
        selectionAnchorKey = nil
    }

    private func openNoteEditor(for verse: BibleVerse) {
        noteDraft = environment.bookmark(for: verse)?.note ?? ""
        noteEditingVerse = verse
    }

    private func copySelectedVerses() {
        let text = selectedVersesShareText
        guard !text.isEmpty else { return }
        RecordClipboard.copy(text)
        clearVerseSelection()
        copiedNotice = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            copiedNotice = false
        }
    }

    private var pinchGesture: some Gesture {
        MagnificationGesture()
            .onChanged { scale in
                guard environment.readingStyle.multitouchZoomEnabled else { return }
                if pinchAnchorFontSize == nil {
                    pinchAnchorFontSize = environment.readingStyle.fontSize
                }
                let base = pinchAnchorFontSize ?? 18
                let updated = min(
                    max(base * Double(scale), ReadingStyle.fontSizeRange.lowerBound),
                    ReadingStyle.fontSizeRange.upperBound
                )
                environment.updateStyle(environment.readingStyle.withFontSize(updated))
                fontSizeHint = Int(updated.rounded())
            }
            .onEnded { _ in
                pinchAnchorFontSize = nil
                let hint = fontSizeHint
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    if fontSizeHint == hint {
                        withAnimation { fontSizeHint = nil }
                    }
                }
            }
    }

    private var chapterSwipeGesture: some Gesture {
        DragGesture(minimumDistance: 40, coordinateSpace: .local)
            .onChanged { value in
                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                swipeOffset = value.translation.width
            }
            .onEnded { value in
                defer {
                    withAnimation(.easeOut(duration: 0.18)) { swipeOffset = 0 }
                }
                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                if value.translation.width < -80 {
                    withAnimation(.easeInOut(duration: 0.22)) {
                        viewModel.moveChapter(by: 1)
                    }
                } else if value.translation.width > 80 {
                    withAnimation(.easeInOut(duration: 0.22)) {
                        viewModel.moveChapter(by: -1)
                    }
                }
            }
    }

    private var readerToolbar: some View {
        VStack(spacing: 8) {
            HStack {
                Button {
                    showBookPicker = true
                } label: {
                    Label("\(viewModel.currentBook.koreanName) \(viewModel.chapter)장", systemImage: "list.bullet")
                        .font(.subheadline.weight(.semibold))
                }
                Spacer()
                chapterNavButton(systemName: "chevron.left") {
                    viewModel.moveChapter(by: -1)
                }
                chapterNavButton(systemName: "chevron.right") {
                    viewModel.moveChapter(by: 1)
                }
            }
            HStack {
                Button(viewModel.selectedVersion?.displayName ?? "역본") { showVersionPicker = true }
                    .buttonStyle(.bordered)
                Button(viewModel.comparisonVersion?.displayName ?? "비교") { showComparisonPicker = true }
                    .buttonStyle(.bordered)
                Spacer()
            }
            if let message = environment.cacheWarmUpMessage, let progress = environment.cacheWarmUpProgress {
                ProgressView(value: progress) {
                    Text(message).font(.caption)
                }
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }

    private func chapterNavButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.title3.weight(.semibold))
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(palette.primary)
    }

    private var chapterContentID: String {
        [
            viewModel.selectedVersion?.id ?? "none",
            String(viewModel.bookIndex),
            String(viewModel.chapter),
            String(viewModel.verses.count),
            viewModel.verses.first?.text ?? "",
        ].joined(separator: "-")
    }

    private var readerRefreshKey: String {
        let folder = environment.dataFolderURL?.path ?? "none"
        return "\(environment.isBootstrapped)-\(folder)"
    }

    private var verseSpacing: CGFloat {
        max(4, environment.readingStyle.fontSize * 0.25)
    }

    private func comparisonText(for verse: Int) -> String? {
        viewModel.comparisonVerses.first { $0.verse == verse }?.text
    }
}

private struct VerseCardView: View {
    let verse: BibleVerse
    let comparisonText: String?
    let fontSize: Double
    let lineHeight: Double
    let bold: Bool
    let bookmark: VerseBookmark?
    let showNoteIcon: Bool
    let isSelected: Bool
    let isSelectionActive: Bool
    let onBookmark: () -> Void
    let onHighlight: () -> Void
    let onHighlightLongPress: () -> Void
    let onRead: () -> Void
    let onNote: () -> Void
    let onSelectToggle: () -> Void

    @Environment(\.readingPalette) private var palette

    var body: some View {
        VStack(alignment: .leading, spacing: max(4, fontSize * 0.18)) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text("\(verse.verse)")
                    .font(.system(size: fontSize * 1.1, weight: .bold))
                    .foregroundStyle(actionForeground)
                Text(verse.text)
                    .font(.system(size: fontSize, weight: bold ? .semibold : .regular))
                    .lineSpacing(fontSize * (lineHeight - 1))
                if showNoteIcon, let note = bookmark?.note, !note.isEmpty {
                    Image(systemName: "note.text")
                        .foregroundStyle(palette.primary)
                }
            }
            if let comparisonText {
                Text(comparisonText)
                    .font(.system(size: fontSize * 0.92))
                    .foregroundStyle(cardForeground.opacity(0.75))
                    .padding(.leading, fontSize * 1.4)
            }
            HStack(spacing: 0) {
                verseActionButton(
                    systemName: bookmark?.isBookmarked == true ? "bookmark.fill" : "bookmark",
                    action: onBookmark
                )
                highlightActionButton
                verseActionButton(
                    systemName: bookmark?.isRead == true ? "checkmark.circle.fill" : "circle",
                    action: onRead
                )
                verseActionButton(
                    systemName: (bookmark?.note.isEmpty == false) ? "note.text.badge.plus" : "note.text",
                    action: onNote
                )
            }
            .foregroundStyle(actionForeground)
        }
        .foregroundStyle(cardForeground)
        .padding(max(6, fontSize * 0.35))
        .background(cardBackground, in: RoundedRectangle(cornerRadius: 12))
        .overlay {
            if isSelected {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(palette.primary, lineWidth: 2)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            if isSelectionActive {
                onSelectToggle()
            }
        }
        .onLongPressGesture {
            onSelectToggle()
        }
    }

    private var highlightActionButton: some View {
        Button(action: onHighlight) {
            Image(systemName: "highlighter")
                .font(.system(size: actionIconSize, weight: .medium))
                .foregroundStyle(highlightColor)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.35).onEnded { _ in
                onHighlightLongPress()
            }
        )
    }

    private var actionIconSize: CGFloat {
        max(22, fontSize * 0.78)
    }

    private func verseActionButton(systemName: String, color: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: actionIconSize, weight: .medium))
                .foregroundStyle(color ?? actionForeground)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var highlightColor: Color {
        guard isHighlighted else { return palette.primary }
        return actionForeground
    }

    private var cardBackground: Color {
        if isSelected {
            return palette.primary.opacity(0.12)
        }
        guard let highlight = bookmark?.highlight, highlight != .none else {
            return palette.surface.opacity(0.6)
        }
        return palette.highlight[highlight] ?? palette.surface.opacity(0.6)
    }

    private var isHighlighted: Bool {
        bookmark?.highlight != nil && bookmark?.highlight != .none
    }

    private var cardForeground: Color {
        if isSelected { return palette.onSurface }
        if isHighlighted { return palette.isDark ? .white : .black }
        return palette.onSurface
    }

    private var actionForeground: Color {
        isHighlighted ? cardForeground : palette.primary
    }
}

private extension ReadingStyle {
    func withFontSize(_ value: Double) -> ReadingStyle {
        var copy = self
        copy.fontSize = value
        return copy
    }
}
