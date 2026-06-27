import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var environment: AppEnvironment
    @Environment(\.readingPalette) private var palette
    @ObservedObject var viewModel: SearchViewModel
    var onOpenResult: (BibleSearchResult) -> Void

    @ObservedObject var readerViewModel: ReaderViewModel

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                TextField("검색어 (2글자 이상)", text: $viewModel.query)
                    .textFieldStyle(ReadingTextFieldStyle())
                    .submitLabel(.search)
                    .onSubmit {
                        KeyboardDismiss.hide()
                        Task { await runSearch() }
                    }
                Button("검색") {
                    KeyboardDismiss.hide()
                    Task { await runSearch() }
                }
                    .buttonStyle(ReadingPrimaryButtonStyle())
            }
            .padding(.horizontal)

            if let version = viewModel.selectedVersion {
                Text("역본: \(version.displayName)")
                    .font(.caption)
                    .readingSecondaryForeground()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal)
            }

            if viewModel.isSearching {
                ProgressView(viewModel.progressLabel, value: viewModel.progressValue)
                    .tint(palette.primary)
                    .readingSecondaryForeground()
                    .padding(.horizontal)
            }

            List {
                Section {
                    Text("결과 \(viewModel.results.count)개")
                        .font(.caption)
                        .readingSecondaryForeground()
                }
                ForEach(viewModel.pagedResults) { result in
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(result.book.koreanName) \(result.verse.chapter):\(result.verse.verse) · \(result.verse.versionCode)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(palette.primary)
                        Text(highlightedSnippet(result.snippet, query: viewModel.query))
                            .font(.body)
                            .foregroundStyle(palette.onSurface)
                    }
                    .contentShape(Rectangle())
                    .onLongPressGesture { onOpenResult(result) }
                }
                if viewModel.canLoadMore {
                    Button("더 보기") { viewModel.resultPage += 1 }
                        .foregroundStyle(palette.primary)
                }
            }
            .listStyle(.plain)
            .readingThemedList()
            .dismissKeyboardOnScroll()
            .contentMargins(.bottom, 8, for: .scrollContent)
        }
        .background(palette.background)
        .onAppear {
            viewModel.syncVersions(from: readerViewModel.versions)
        }
        .onChange(of: readerViewModel.selectedVersion?.code) { _, code in
            viewModel.syncReaderVersionCode(code)
            viewModel.syncVersions(from: readerViewModel.versions)
        }
    }

    private func runSearch() async {
        await viewModel.search(environment: environment)
    }

    private func highlightedSnippet(_ text: String, query: String) -> AttributedString {
        var attributed = AttributedString(text)
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              let range = attributed.range(of: trimmed, options: .caseInsensitive)
        else { return attributed }
        attributed[range].font = .body.bold()
        attributed[range].foregroundColor = palette.onSurface
        return attributed
    }
}
