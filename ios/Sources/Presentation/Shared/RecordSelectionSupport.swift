import SwiftUI

struct HighlightColorPickerSheet: View {
    @Binding var selection: VerseHighlight
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    private let colors: [VerseHighlight] = [.yellow, .mint, .blue, .pink, .lavender, .orange]

    var body: some View {
        NavigationStack {
            List {
                ForEach(colors, id: \.self) { color in
                    Button {
                        selection = color
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
            .navigationTitle("형광펜 색상")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(palette.background, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("닫기") { dismiss() }
                }
            }
        }
    }
}

struct SelectionActionBar: View {
    let count: Int
    let onClear: () -> Void
    let onCopy: () -> Void
    let onShare: () -> Void
    @Environment(\.readingPalette) private var palette

    var body: some View {
        HStack(spacing: 10) {
            Text("\(count)개 선택")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(palette.onSurface)
            Spacer()
            Button("해제", action: onClear)
                .buttonStyle(ReadingSecondaryButtonStyle())
            Button("복사", action: onCopy)
                .buttonStyle(ReadingSecondaryButtonStyle())
            Button("공유", action: onShare)
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
