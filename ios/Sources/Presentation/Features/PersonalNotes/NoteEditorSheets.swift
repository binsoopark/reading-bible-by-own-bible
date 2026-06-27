import SwiftUI
import UniformTypeIdentifiers

struct VerseNoteEditorSheet: View {
    let reference: String
    let verseText: String
    @Binding var note: String
    let onSave: () -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(reference)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(palette.primary)
                    Text(verseText)
                        .font(.subheadline)
                        .foregroundStyle(palette.onSurface)
                    TextField("이 구절에 대한 메모", text: $note, axis: .vertical)
                        .textFieldStyle(ReadingTextFieldStyle())
                        .lineLimit(5...12)
                }
                .padding()
            }
            .background(palette.background)
            .navigationTitle("구절 메모")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("저장") {
                        onSave()
                        dismiss()
                    }
                }
            }
        }
    }
}

struct PersonalNoteEditorSheet: View {
    let title: String
    @State private var noteTitle: String
    @State private var noteBody: String
    let onSave: (String, String) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    init(title: String, initialTitle: String, initialBody: String, onSave: @escaping (String, String) -> Void) {
        self.title = title
        _noteTitle = State(initialValue: initialTitle)
        _noteBody = State(initialValue: initialBody)
        self.onSave = onSave
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("제목", text: $noteTitle)
                    .textFieldStyle(ReadingTextFieldStyle())
                TextField("내용", text: $noteBody, axis: .vertical)
                    .textFieldStyle(ReadingTextFieldStyle())
                    .lineLimit(6...20)
            }
            .readingThemedForm()
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("저장") {
                        onSave(noteTitle, noteBody)
                        dismiss()
                    }
                }
            }
        }
    }
}

struct PersonalNoteDocumentExporter: UIViewControllerRepresentable {
    let data: Data
    let filename: String

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(filename)
        try? data.write(to: url)
        let picker = UIDocumentPickerViewController(forExporting: [url], asCopy: true)
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
}

struct PersonalNoteDocumentImporter: UIViewControllerRepresentable {
    let onImport: (Data) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.json, .data], asCopy: true)
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onImport: onImport) }

    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onImport: (Data) -> Void
        init(onImport: @escaping (Data) -> Void) { self.onImport = onImport }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first, let data = try? Data(contentsOf: url) else { return }
            onImport(data)
        }
    }
}
