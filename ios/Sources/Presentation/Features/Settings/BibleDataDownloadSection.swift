import SwiftUI

struct BibleDataDownloadSection: View {
    @EnvironmentObject private var environment: AppEnvironment
    @Environment(\.readingPalette) private var palette
    @State private var expanded = false

    var body: some View {
        Section {
            HStack {
                Text(L10n.text("download.title"))
                    .font(.headline)
                Spacer()
                Button {
                    withAnimation { expanded.toggle() }
                } label: {
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .foregroundStyle(palette.primary)
                }
                .buttonStyle(.plain)
            }

            Text(
                L10n.text("download.notice")
            )
            .font(.subheadline)
            .readingSecondaryForeground()

            if expanded {
                Text(
                    L10n.text("download.detail")
                )
                .font(.caption)
                .readingSecondaryForeground()

                if let root = environment.dataDownloadState.installedRoot {
                    Text(L10n.format("download.installedFolder", root.path))
                        .font(.caption2)
                        .readingSecondaryForeground()
                }

                if !environment.dataDownloadState.message.isEmpty {
                    Text(environment.dataDownloadState.message)
                        .font(.caption)
                        .foregroundStyle(messageColor)
                }

                if let progress = environment.dataDownloadState.progress {
                    ProgressView(value: progress)
                        .tint(palette.primary)
                }

                Button(L10n.text("download.action")) {
                    Task { await environment.downloadSampleBibleData() }
                }
                .buttonStyle(ReadingPrimaryButtonStyle())
                .disabled(environment.dataDownloadState.isActive)
            }
        } header: {
            EmptyView()
        }
    }

    private var messageColor: Color {
        if environment.dataDownloadState.isError {
            return .red
        }
        return palette.onSurfaceVariant
    }
}
