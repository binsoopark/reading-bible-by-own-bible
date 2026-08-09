import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @EnvironmentObject private var environment: AppEnvironment
    @Environment(\.readingPalette) private var palette
    @State private var showFolderPicker = false
    @State private var diagnostic: BibleFileDiagnostic?
    @State private var showDiagnostics = false
    @State private var importJSON = ""
    @State private var showPalettePicker = false
    @State private var showHelpGuide = false

    var body: some View {
        VStack(spacing: 0) {
            Text(L10n.text("tab.settings"))
                .font(.title3.weight(.semibold))
                .foregroundStyle(palette.onSurface)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 4)

            Form {
                Section {
                    Button {
                        showHelpGuide = true
                    } label: {
                        HStack {
                            Label(L10n.text("settings.help.title"), systemImage: "questionmark.circle")
                                .foregroundStyle(palette.onSurface)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(palette.onSurfaceVariant)
                        }
                        .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Text(L10n.text("settings.help.description"))
                        .font(.caption)
                        .readingSecondaryForeground()
                } header: {
                    Text(L10n.text("settings.help.section")).foregroundStyle(palette.onSurfaceVariant)
                }

                Section {
                    Button(L10n.text("settings.chooseFolder")) { showFolderPicker = true }
                    if let url = environment.dataFolderURL {
                        Text(url.path)
                            .font(.caption)
                            .readingSecondaryForeground()
                    } else {
                        Text(L10n.text("settings.noFolder"))
                            .readingSecondaryForeground()
                    }
                    Button(L10n.text("settings.checkFiles")) { Task { await loadDiagnostics() } }
                } header: {
                    Text(L10n.text("settings.data.section")).foregroundStyle(palette.onSurfaceVariant)
                }

                BibleDataDownloadSection()

                Section {
                    Slider(value: fontSizeBinding, in: ReadingStyle.fontSizeRange, step: 1) {
                        Text(L10n.text("settings.fontSize"))
                    }
                    .tint(palette.primary)
                    Slider(value: lineHeightBinding, in: ReadingStyle.lineHeightRange, step: 0.05) {
                        Text(L10n.text("settings.lineSpacing"))
                    }
                    .tint(palette.primary)
                    Button {
                        showPalettePicker = true
                    } label: {
                        HStack {
                            Text(L10n.text("settings.palette"))
                                .foregroundStyle(palette.onSurface)
                            Spacer()
                            Text(environment.readingStyle.palette.label)
                                .foregroundStyle(palette.onSurfaceVariant)
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(palette.onSurfaceVariant)
                        }
                        .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Toggle(L10n.text("settings.keepScreenOn"), isOn: keepScreenOnBinding)
                    Toggle(L10n.text("settings.multitouchZoom"), isOn: multitouchZoomBinding)
                    Toggle(L10n.text("settings.boldText"), isOn: boldBinding)
                    Toggle(L10n.text("settings.showNoteIcon"), isOn: noteIconBinding)
                } header: {
                    Text(L10n.text("settings.readingStyle.section")).foregroundStyle(palette.onSurfaceVariant)
                }

                Section {
                    ShareLink(item: environment.exportRecordsJSON(), preview: SharePreview(L10n.text("settings.records.sharePreview")))
                    TextField(L10n.text("settings.records.importPlaceholder"), text: $importJSON, axis: .vertical)
                        .textFieldStyle(ReadingTextFieldStyle())
                    Button(L10n.text("settings.records.import")) {
                        KeyboardDismiss.hide()
                        environment.importRecordsJSON(importJSON)
                    }
                } header: {
                    Text(L10n.text("settings.records.section")).foregroundStyle(palette.onSurfaceVariant)
                }

                Section {
                    Text(appInfoText)
                        .font(.footnote)
                        .readingSecondaryForeground()
                    LabeledContent(L10n.text("settings.version"), value: "1.0.0")
                } header: {
                    Text(L10n.text("settings.appInfo.section")).foregroundStyle(palette.onSurfaceVariant)
                }
            }
            .readingThemedForm()
            .dismissKeyboardOnScroll()
        }
        .background(palette.background)
        .sheet(isPresented: $showHelpGuide) {
            HelpGuideView()
                .readingTheme(environment.readingStyle.palette)
        }
        .sheet(isPresented: $showFolderPicker) {
            DocumentFolderPicker { url in
                environment.setDataFolder(url)
            }
        }
        .sheet(isPresented: $showDiagnostics) {
            DiagnosticsSheet(diagnostic: diagnostic)
                .readingTheme(environment.readingStyle.palette)
        }
        .sheet(isPresented: $showPalettePicker) {
            PalettePickerSheet(
                selection: environment.readingStyle.palette,
                onSelect: { palette in
                    environment.updateStyle(environment.readingStyle.withPalette(palette))
                    showPalettePicker = false
                }
            )
            .readingTheme(environment.readingStyle.palette)
        }
    }

    private var appInfoText: String {
        """
        이 앱은 기존 Lifove Bible에서 사용되던 bdf/lfa 형식의 성경 데이터 파일을 보유한 사용자가
        자신의 파일을 불러와 읽을 수 있도록 만든 독립적인 성경 읽기 앱입니다.
        이 앱 자체는 성경 번역 데이터를 포함하지 않습니다.
        """
    }

    private func loadDiagnostics() async {
        guard let repository = environment.repository,
              let root = environment.dataFolderURL ?? environment.dataFolderStore.defaultBibleFolder()
        else { return }
        let scoped = environment.dataFolderStore.startAccessing() ?? root
        defer { environment.dataFolderStore.stopAccessing(scoped) }
        diagnostic = try? await repository.diagnose(at: scoped)
        showDiagnostics = true
    }

    private var fontSizeBinding: Binding<Double> {
        Binding(
            get: { environment.readingStyle.fontSize },
            set: { environment.updateStyle(environment.readingStyle.withFontSize($0)) }
        )
    }

    private var lineHeightBinding: Binding<Double> {
        Binding(
            get: { environment.readingStyle.lineHeightMultiplier },
            set: { environment.updateStyle(environment.readingStyle.withLineHeight($0)) }
        )
    }

    private var keepScreenOnBinding: Binding<Bool> {
        Binding(
            get: { environment.readingStyle.keepScreenOn },
            set: { environment.updateStyle(environment.readingStyle.withKeepScreenOn($0)) }
        )
    }

    private var multitouchZoomBinding: Binding<Bool> {
        Binding(
            get: { environment.readingStyle.multitouchZoomEnabled },
            set: { environment.updateStyle(environment.readingStyle.withMultitouchZoom($0)) }
        )
    }

    private var boldBinding: Binding<Bool> {
        Binding(
            get: { environment.readingStyle.boldTextEnabled },
            set: { environment.updateStyle(environment.readingStyle.withBoldText($0)) }
        )
    }

    private var noteIconBinding: Binding<Bool> {
        Binding(
            get: { environment.readingStyle.showNotesInReader },
            set: { environment.updateStyle(environment.readingStyle.withShowNotes($0)) }
        )
    }
}

private struct PalettePickerSheet: View {
    let selection: ReadingPalette
    let onSelect: (ReadingPalette) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    var body: some View {
        NavigationStack {
            List {
                ForEach(ReadingPalette.allCases, id: \.self) { option in
                    Button {
                        onSelect(option)
                    } label: {
                        HStack {
                            Text(option.label)
                                .foregroundStyle(palette.onSurface)
                            Spacer()
                            if selection == option {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(palette.primary)
                            }
                        }
                        .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .readingThemedList()
            .navigationTitle(L10n.text("settings.palette"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(palette.background, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.text("action.close")) { dismiss() }
                }
            }
        }
    }
}

private struct DiagnosticsSheet: View {
    let diagnostic: BibleFileDiagnostic?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    var body: some View {
        NavigationStack {
            List {
                if let diagnostic {
                    Section(L10n.text("diagnostics.summary")) {
                        LabeledContent(L10n.text("diagnostics.fileCount"), value: "\(diagnostic.scannedFileCount)")
                        LabeledContent("LFA", value: "\(diagnostic.lfaCount)")
                        LabeledContent("BDF", value: "\(diagnostic.bdfFileCount)")
                        LabeledContent(L10n.text("diagnostics.completeBdf"), value: "\(diagnostic.completeBdfVersionCount)")
                        LabeledContent("MP3", value: "\(diagnostic.mp3Count)")
                    }
                    Section(L10n.text("diagnostics.versions")) {
                        ForEach(diagnostic.versions) { version in
                            Text("\(version.displayName) (\(version.code))")
                        }
                    }
                    Section(L10n.text("diagnostics.issues")) {
                        ForEach(diagnostic.issues) { issue in
                            VStack(alignment: .leading) {
                                Text(issue.title).font(.headline)
                                Text(issue.detail).font(.caption).readingSecondaryForeground()
                            }
                        }
                    }
                }
            }
            .readingThemedList()
            .navigationTitle(L10n.text("diagnostics.title"))
            .toolbarBackground(palette.background, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.text("action.close")) { dismiss() }
                }
            }
        }
    }
}

struct DocumentFolderPicker: UIViewControllerRepresentable {
    let onPick: (URL) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.folder])
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onPick: onPick) }

    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onPick: (URL) -> Void
        init(onPick: @escaping (URL) -> Void) { self.onPick = onPick }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }
            onPick(url)
        }
    }
}

private extension ReadingStyle {
    func withFontSize(_ value: Double) -> ReadingStyle {
        var copy = self; copy.fontSize = value; return copy
    }
    func withLineHeight(_ value: Double) -> ReadingStyle {
        var copy = self; copy.lineHeightMultiplier = value; return copy
    }
    func withPalette(_ value: ReadingPalette) -> ReadingStyle {
        var copy = self; copy.palette = value; return copy
    }
    func withKeepScreenOn(_ value: Bool) -> ReadingStyle {
        var copy = self; copy.keepScreenOn = value; return copy
    }
    func withMultitouchZoom(_ value: Bool) -> ReadingStyle {
        var copy = self; copy.multitouchZoomEnabled = value; return copy
    }
    func withBoldText(_ value: Bool) -> ReadingStyle {
        var copy = self; copy.boldTextEnabled = value; return copy
    }
    func withShowNotes(_ value: Bool) -> ReadingStyle {
        var copy = self; copy.showNotesInReader = value; return copy
    }
}
