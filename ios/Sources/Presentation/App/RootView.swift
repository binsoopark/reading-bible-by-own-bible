import SwiftUI

struct RootView: View {
    @EnvironmentObject private var environment: AppEnvironment
    @StateObject private var readerViewModel = ReaderViewModel()
    @StateObject private var searchViewModel = SearchViewModel()
    @State private var selectedTab: AppTab = .reader

    var body: some View {
        tabContent
            .safeAreaInset(edge: .bottom, spacing: 0) {
                tabBarChrome
            }
    }

    private var tabContent: some View {
        Group {
            switch selectedTab {
            case .reader:
                ReaderView(viewModel: readerViewModel)
            case .search:
                SearchView(viewModel: searchViewModel, onOpenResult: { result in
                    readerViewModel.openSearchResult(result.verse)
                    selectedTab = .reader
                }, readerViewModel: readerViewModel)
            case .records:
                RecordsView(onOpenBookmark: { bookmark in
                    readerViewModel.openBookmark(bookmark)
                    selectedTab = .reader
                })
            case .personalNotes:
                PersonalNotesView()
            case .settings:
                SettingsView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .readingTheme(environment.readingStyle.palette)
        .dismissKeyboardOnTap()
        .onChange(of: selectedTab) { _, _ in
            KeyboardDismiss.hide()
        }
        .onAppear {
            readerViewModel.configure(environment: environment)
            searchViewModel.configure(environment: environment, readerVersionCode: readerViewModel.selectedVersion?.code)
        }
        .onChange(of: environment.isBootstrapped) { _, isReady in
            guard isReady else { return }
            readerViewModel.configure(environment: environment)
            searchViewModel.configure(
                environment: environment,
                readerVersionCode: readerViewModel.selectedVersion?.code
            )
        }
        .onChange(of: readerViewModel.versions.map(\.id)) { _, _ in
            searchViewModel.configure(
                environment: environment,
                readerVersionCode: readerViewModel.selectedVersion?.code
            )
            searchViewModel.syncVersions(from: readerViewModel.versions)
        }
    }

    private var tabBarChrome: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [.clear, ReadingPaletteColors.colors(for: environment.readingStyle.palette).background],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: FloatingTabBarLayout.fadeHeight)
            .allowsHitTesting(false)

            FloatingTabBar(selection: $selectedTab)
        }
        .background(ReadingPaletteColors.colors(for: environment.readingStyle.palette).background)
    }
}
