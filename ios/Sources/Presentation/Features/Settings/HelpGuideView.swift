import SwiftUI

struct HelpGuideView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(HelpGuideContent.intro)
                        .font(.subheadline)
                        .foregroundStyle(palette.onSurface)
                        .fixedSize(horizontal: false, vertical: true)
                } header: {
                    Text("시작하기").foregroundStyle(palette.onSurfaceVariant)
                }

                ForEach(HelpGuideContent.sections) { section in
                    Section {
                        ForEach(section.items) { item in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.title)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(palette.primary)
                                Text(item.detail)
                                    .font(.subheadline)
                                    .foregroundStyle(palette.onSurface)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            .padding(.vertical, 4)
                        }
                    } header: {
                        Text(section.title).foregroundStyle(palette.onSurfaceVariant)
                    }
                }
            }
            .readingThemedList()
            .navigationTitle("사용 방법")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(palette.background, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("닫기") { dismiss() }
                }
            }
        }
    }
}
