import SwiftUI

struct BibleDataDownloadSection: View {
    @EnvironmentObject private var environment: AppEnvironment
    @Environment(\.readingPalette) private var palette
    @State private var expanded = false

    var body: some View {
        Section {
            HStack {
                Text("성경 데이터 다운로드")
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
                "사용자의 성경 데이터가 없을 경우 편의를 위해 별도의 성경 데이터 다운로드 기능을 제공합니다. " +
                "성경 데이터를 구할 수 없는 경우에 한해, 제한적으로 다운로드를 이용해 주세요."
            )
            .font(.subheadline)
            .readingSecondaryForeground()

            if expanded {
                Text(
                    "GitHub Release에서 약 49MB의 bible.zip 파일을 내려받아 앱 전용 폴더에 압축 해제하고 바로 적용합니다. " +
                    "이미 bdf/lfa 폴더를 선택했거나 성경 데이터가 감지되면 다운로드할 수 없습니다."
                )
                .font(.caption)
                .readingSecondaryForeground()

                if let root = environment.dataDownloadState.installedRoot {
                    Text("적용된 폴더: \(root.path)")
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

                Button("다운로드 및 적용") {
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
        let message = environment.dataDownloadState.message
        if message.contains("실패") || message.contains("이미") {
            return .red
        }
        return palette.onSurfaceVariant
    }
}
