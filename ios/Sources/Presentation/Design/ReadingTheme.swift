import SwiftUI

struct ReadingPaletteColors {
    let background: Color
    let surface: Color
    let surfaceContainer: Color
    let fieldBackground: Color
    let primary: Color
    let onSurface: Color
    let onSurfaceVariant: Color
    let verseNumber: Color
    let highlight: [VerseHighlight: Color]
    let isDark: Bool

    var preferredColorScheme: ColorScheme {
        isDark ? .dark : .light
    }

    static func colors(for palette: ReadingPalette) -> ReadingPaletteColors {
        switch palette {
        case .paper:
            return ReadingPaletteColors(
                background: Color(red: 0.98, green: 0.98, blue: 0.96),
                surface: Color(red: 0.98, green: 0.98, blue: 0.96),
                surfaceContainer: Color.white,
                fieldBackground: Color(red: 0.96, green: 0.95, blue: 0.92),
                primary: Color(red: 0.11, green: 0.42, blue: 0.38),
                onSurface: Color(red: 0.11, green: 0.11, blue: 0.09),
                onSurfaceVariant: Color(red: 0.35, green: 0.34, blue: 0.32),
                verseNumber: Color(red: 0.11, green: 0.42, blue: 0.38),
                highlight: highlightMap(
                    yellow: Color.yellow.opacity(0.35),
                    mint: Color.mint.opacity(0.35),
                    blue: Color.blue.opacity(0.25),
                    pink: Color.pink.opacity(0.25),
                    lavender: Color.purple.opacity(0.22),
                    orange: Color.orange.opacity(0.28)
                ),
                isDark: false
            )
        case .evening:
            return ReadingPaletteColors(
                background: Color(red: 0.09, green: 0.08, blue: 0.07),
                surface: Color(red: 0.13, green: 0.12, blue: 0.10),
                surfaceContainer: Color(red: 0.16, green: 0.15, blue: 0.13),
                fieldBackground: Color(red: 0.20, green: 0.18, blue: 0.16),
                primary: Color(red: 0.88, green: 0.71, blue: 0.42),
                onSurface: Color(red: 0.95, green: 0.91, blue: 0.85),
                onSurfaceVariant: Color(red: 0.72, green: 0.68, blue: 0.62),
                verseNumber: Color(red: 0.88, green: 0.71, blue: 0.42),
                highlight: highlightMap(
                    yellow: Color.yellow.opacity(0.22),
                    mint: Color.mint.opacity(0.22),
                    blue: Color.blue.opacity(0.22),
                    pink: Color.pink.opacity(0.22),
                    lavender: Color.purple.opacity(0.22),
                    orange: Color.orange.opacity(0.22)
                ),
                isDark: true
            )
        case .oled:
            return ReadingPaletteColors(
                background: .black,
                surface: .black,
                surfaceContainer: Color(red: 0.12, green: 0.12, blue: 0.12),
                fieldBackground: Color(red: 0.18, green: 0.18, blue: 0.18),
                primary: Color(red: 0.47, green: 0.84, blue: 0.78),
                onSurface: Color(red: 0.93, green: 0.93, blue: 0.93),
                onSurfaceVariant: Color(red: 0.65, green: 0.65, blue: 0.65),
                verseNumber: Color(red: 0.47, green: 0.84, blue: 0.78),
                highlight: highlightMap(
                    yellow: Color.yellow.opacity(0.18),
                    mint: Color.mint.opacity(0.18),
                    blue: Color.blue.opacity(0.18),
                    pink: Color.pink.opacity(0.18),
                    lavender: Color.purple.opacity(0.18),
                    orange: Color.orange.opacity(0.18)
                ),
                isDark: true
            )
        case .highContrast:
            return ReadingPaletteColors(
                background: .white,
                surface: .white,
                surfaceContainer: Color(red: 0.97, green: 0.97, blue: 0.97),
                fieldBackground: Color(red: 0.94, green: 0.94, blue: 0.94),
                primary: Color(red: 0.0, green: 0.36, blue: 0.83),
                onSurface: .black,
                onSurfaceVariant: Color(red: 0.25, green: 0.25, blue: 0.25),
                verseNumber: Color(red: 0.0, green: 0.36, blue: 0.83),
                highlight: highlightMap(
                    yellow: Color.yellow.opacity(0.55),
                    mint: Color.green.opacity(0.35),
                    blue: Color.blue.opacity(0.35),
                    pink: Color.pink.opacity(0.35),
                    lavender: Color.purple.opacity(0.35),
                    orange: Color.orange.opacity(0.35)
                ),
                isDark: false
            )
        case .warmLight:
            return ReadingPaletteColors(
                background: Color(red: 1.0, green: 0.96, blue: 0.91),
                surface: Color(red: 1.0, green: 0.95, blue: 0.84),
                surfaceContainer: Color(red: 1.0, green: 0.93, blue: 0.80),
                fieldBackground: Color(red: 0.98, green: 0.90, blue: 0.78),
                primary: Color(red: 0.60, green: 0.36, blue: 0.0),
                onSurface: Color(red: 0.18, green: 0.13, blue: 0.08),
                onSurfaceVariant: Color(red: 0.45, green: 0.34, blue: 0.22),
                verseNumber: Color(red: 0.60, green: 0.36, blue: 0.0),
                highlight: highlightMap(
                    yellow: Color.yellow.opacity(0.35),
                    mint: Color.mint.opacity(0.30),
                    blue: Color.blue.opacity(0.22),
                    pink: Color.pink.opacity(0.22),
                    lavender: Color.purple.opacity(0.22),
                    orange: Color.orange.opacity(0.30)
                ),
                isDark: false
            )
        }
    }

    private static func highlightMap(
        yellow: Color, mint: Color, blue: Color, pink: Color, lavender: Color, orange: Color
    ) -> [VerseHighlight: Color] {
        [
            .yellow: yellow, .mint: mint, .blue: blue,
            .pink: pink, .lavender: lavender, .orange: orange,
        ]
    }
}

private struct PaletteEnvironmentKey: EnvironmentKey {
    static let defaultValue = ReadingPaletteColors.colors(for: .paper)
}

extension EnvironmentValues {
    var readingPalette: ReadingPaletteColors {
        get { self[PaletteEnvironmentKey.self] }
        set { self[PaletteEnvironmentKey.self] = newValue }
    }
}

struct ReadingThemeModifier: ViewModifier {
    let palette: ReadingPalette

    private var colors: ReadingPaletteColors {
        ReadingPaletteColors.colors(for: palette)
    }

    func body(content: Content) -> some View {
        content
            .environment(\.readingPalette, colors)
            .background(colors.background)
            .foregroundStyle(colors.onSurface)
            .tint(colors.primary)
            .preferredColorScheme(colors.preferredColorScheme)
    }
}

struct ReadingThemedListModifier: ViewModifier {
    @Environment(\.readingPalette) private var palette

    func body(content: Content) -> some View {
        content
            .scrollContentBackground(.hidden)
            .background(palette.background)
            .listRowBackground(palette.surfaceContainer)
            .listRowSeparatorTint(palette.onSurface.opacity(0.12))
    }
}

struct ReadingThemedFormModifier: ViewModifier {
    @Environment(\.readingPalette) private var palette

    func body(content: Content) -> some View {
        content
            .scrollContentBackground(.hidden)
            .background(palette.background)
            .listRowBackground(palette.surfaceContainer)
            .listSectionSeparatorTint(palette.onSurface.opacity(0.12))
    }
}

struct ReadingTextFieldStyle: TextFieldStyle {
    @Environment(\.readingPalette) private var palette

    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(10)
            .background(palette.fieldBackground, in: RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(palette.onSurface.opacity(0.12), lineWidth: 1)
            )
            .foregroundStyle(palette.onSurface)
    }
}

extension View {
    func readingTheme(_ palette: ReadingPalette) -> some View {
        modifier(ReadingThemeModifier(palette: palette))
    }

    func readingThemedList() -> some View {
        modifier(ReadingThemedListModifier())
    }

    func readingThemedForm() -> some View {
        modifier(ReadingThemedFormModifier())
    }

    func readingSecondaryForeground() -> some View {
        modifier(ReadingSecondaryForegroundModifier())
    }
}

private struct ReadingSecondaryForegroundModifier: ViewModifier {
    @Environment(\.readingPalette) private var palette

    func body(content: Content) -> some View {
        content.foregroundStyle(palette.onSurfaceVariant)
    }
}

struct ReadingPrimaryButtonStyle: ButtonStyle {
    @Environment(\.readingPalette) private var palette

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.semibold))
            .foregroundStyle(palette.isDark ? palette.onSurface : .white)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(palette.primary.opacity(configuration.isPressed ? 0.82 : 1), in: Capsule())
    }
}

struct ReadingSecondaryButtonStyle: ButtonStyle {
    @Environment(\.readingPalette) private var palette

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.medium))
            .foregroundStyle(palette.primary)
            .padding(.horizontal, 14)
            .padding(.vertical, 9)
            .background(palette.primary.opacity(configuration.isPressed ? 0.14 : 0.10), in: Capsule())
            .overlay(Capsule().stroke(palette.primary.opacity(0.25), lineWidth: 1))
    }
}
