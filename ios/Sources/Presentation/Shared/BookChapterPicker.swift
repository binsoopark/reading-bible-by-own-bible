import SwiftUI

struct BookChapterPicker: View {
    @Binding var bookIndex: Int
    @Binding var chapter: Int
    let onConfirm: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var testament: Testament = .old

    enum Testament: String, CaseIterable {
        case old = "구약"
        case new = "신약"
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("testament", selection: $testament) {
                    ForEach(Testament.allCases, id: \.self) { value in
                        Text(value.rawValue).tag(value)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)

                Divider()

                HStack(spacing: 0) {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 0) {
                            ForEach(filteredBooks, id: \.index) { item in
                                Button {
                                    bookIndex = item.index
                                    chapter = 1
                                } label: {
                                    HStack {
                                        Text(item.koreanName)
                                        Spacer()
                                        if item.index == bookIndex {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                    .padding(.vertical, 12)
                                    .padding(.horizontal, 12)
                                    .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)

                    Divider()

                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 0) {
                            ForEach(chapters, id: \.self) { value in
                                Button {
                                    chapter = value
                                    onConfirm()
                                    dismiss()
                                } label: {
                                    HStack {
                                        Text("\(value)장")
                                        Spacer()
                                        if value == chapter {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                    .padding(.vertical, 12)
                                    .padding(.horizontal, 12)
                                    .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("책/장 선택")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("취소") { dismiss() }
                }
            }
            .onAppear {
                testament = bookIndex >= 39 ? .new : .old
            }
        }
    }

    private var filteredBooks: [BibleBook] {
        switch testament {
        case .old: return BibleCatalog.books.filter { $0.index < 39 }
        case .new: return BibleCatalog.books.filter { $0.index >= 39 }
        }
    }

    private var chapters: [Int] {
        Array(1 ... BibleCatalog.book(at: bookIndex).chapterCount)
    }
}

struct VersionPicker: View {
    let title: String
    let versions: [BibleVersion]
    @Binding var selection: BibleVersion?
    let allowNone: Bool
    @Environment(\.dismiss) private var dismiss
    @Environment(\.readingPalette) private var palette

    var body: some View {
        NavigationStack {
            Group {
                if versions.isEmpty {
                    ContentUnavailableView(
                        "역본 없음",
                        systemImage: "books.vertical",
                        description: Text("성경 데이터 폴더에서 역본을 찾지 못했습니다.")
                    )
                } else {
                    List {
                        if allowNone {
                            Button("없음") {
                                selection = nil
                                dismiss()
                            }
                            .foregroundStyle(palette.onSurface)
                        }
                        ForEach(groupedVersions.keys.sorted(), id: \.self) { group in
                            Section(group) {
                                ForEach(groupedVersions[group] ?? [], id: \.id) { version in
                                    Button {
                                        selection = version
                                        dismiss()
                                    } label: {
                                        HStack {
                                            VStack(alignment: .leading, spacing: 2) {
                                                Text(version.displayName)
                                                    .foregroundStyle(palette.onSurface)
                                                Text(version.code)
                                                    .font(.caption)
                                                    .readingSecondaryForeground()
                                            }
                                            Spacer()
                                            if selection?.id == version.id {
                                                Image(systemName: "checkmark")
                                                    .foregroundStyle(palette.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .readingThemedList()
                }
            }
            .background(palette.background)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(palette.background, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        }
    }

    private var groupedVersions: [String: [BibleVersion]] {
        Dictionary(grouping: versions) { version in
            let code = version.code.lowercased()
            if code.hasPrefix("kor") || code.contains("korn") || code.contains("kork") { return "한국어" }
            if code.hasPrefix("eng") { return "English" }
            return "기타"
        }
    }
}
