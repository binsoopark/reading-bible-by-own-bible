import SwiftUI

struct HighlightColorPickerSheet: View {
    @Binding var selection: VerseHighlight
    var onSelected: ((VerseHighlight) -> Void)? = nil
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    private let colors: [VerseHighlight] = [.yellow, .mint, .blue, .pink, .lavender, .orange]

    var body: some View {
        NavigationStack {
            List {
                ForEach(colors, id: \.self) { color in
                    Button {
                        selection = color
                        onSelected?(color)
                        dismiss()
                    } label: {
                        HStack(spacing: 12) {
                            Circle()
                                .fill(palette.highlight[color] ?? .yellow)
                                .frame(width: 24, height: 24)
                                .overlay(Circle().stroke(palette.onSurface.opacity(0.15), lineWidth: 1))
                            Text(color.label)
                                .foregroundStyle(palette.onSurface)
                            Spacer()
                            if selection == color {
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
            .navigationTitle(L10n.text("highlight.picker.title"))
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

struct SelectionActionBar: View {
    let count: Int
    let onClear: () -> Void
    var onHighlight: (() -> Void)? = nil
    let onCopy: () -> Void
    let onShare: () -> Void
    @Environment(\.readingPalette) private var palette

    var body: some View {
        HStack(spacing: 10) {
            Text(L10n.format("format.selectedCount", count))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(palette.onSurface)
            Spacer()
            Button(L10n.text("action.clearSelection"), action: onClear)
                .buttonStyle(ReadingSecondaryButtonStyle())
            if let onHighlight {
                Button(action: onHighlight) {
                    Image(systemName: "highlighter")
                }
                .buttonStyle(ReadingSecondaryButtonStyle())
                .accessibilityLabel(L10n.text("highlight.selection.accessibility"))
            }
            Button(L10n.text("action.copy"), action: onCopy)
                .buttonStyle(ReadingSecondaryButtonStyle())
            Button(L10n.text("action.share"), action: onShare)
                .buttonStyle(ReadingPrimaryButtonStyle())
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(palette.surfaceContainer, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(palette.onSurface.opacity(0.10), lineWidth: 1))
    }
}

struct ShareTextSheet: UIViewControllerRepresentable {
    let text: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [text], applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

struct HighlightColorSwatch: View {
    let highlight: VerseHighlight
    @Environment(\.readingPalette) private var palette

    var body: some View {
        Circle()
            .fill(palette.highlight[highlight] ?? .yellow.opacity(0.35))
            .frame(width: 18, height: 18)
            .overlay(Circle().stroke(palette.onSurface.opacity(0.18), lineWidth: 1))
            .accessibilityLabel(highlight.label)
    }
}

enum RecordClipboard {
    static func copy(_ text: String) {
        UIPasteboard.general.string = text
    }
}
