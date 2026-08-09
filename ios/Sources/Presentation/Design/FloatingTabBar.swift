import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case reader
    case search
    case records
    case personalNotes
    case settings

    var id: String { rawValue }

    var label: String {
        switch self {
        case .reader: L10n.text("tab.reader")
        case .search: L10n.text("tab.search")
        case .records: L10n.text("tab.records")
        case .personalNotes: L10n.text("tab.personalNotes")
        case .settings: L10n.text("tab.settings")
        }
    }

    var systemImage: String {
        switch self {
        case .reader: "book"
        case .search: "magnifyingglass"
        case .records: "note.text"
        case .personalNotes: "square.and.pencil"
        case .settings: "gearshape"
        }
    }
}

struct FloatingTabBar: View {
    @Binding var selection: AppTab
    @Environment(\.readingPalette) private var palette

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases) { tab in
                Button {
                    selection = tab
                } label: {
                    VStack(spacing: 2) {
                        Image(systemName: tab.systemImage)
                            .font(.system(size: 15))
                        Text(tab.label)
                            .font(.system(size: 10))
                            .fontWeight(selection == tab ? .semibold : .regular)
                    }
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .padding(.vertical, 4)
                    .background(
                        selection == tab
                            ? palette.primary.opacity(0.15)
                            : Color.clear,
                        in: Capsule()
                    )
                }
                .buttonStyle(.plain)
                .foregroundStyle(selection == tab ? palette.primary : palette.onSurface.opacity(0.65))
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 6)
        .background(palette.surfaceContainer, in: Capsule())
        .overlay(Capsule().stroke(palette.onSurface.opacity(0.10), lineWidth: 1))
        .shadow(color: palette.isDark ? .clear : .black.opacity(0.10), radius: 14, y: 6)
        .padding(.horizontal, 10)
        .padding(.bottom, 8)
    }
}

enum FloatingTabBarLayout {
    static let fadeHeight: CGFloat = 32
}
