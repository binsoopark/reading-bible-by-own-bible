import SwiftUI

private enum PersonalNotesLayout {
    static let fabBottomPadding: CGFloat = 16
    static let swipeActionWidth: CGFloat = 108
}

struct PersonalNotesView: View {
    @EnvironmentObject private var environment: AppEnvironment
    @Environment(\.readingPalette) private var palette
    @State private var selectedIDs: Set<String> = []
    @State private var revealedNoteID: String?
    @State private var editingNote: PersonalNote?
    @State private var isCreating = false
    @State private var showExporter = false
    @State private var exportData = Data()
    @State private var showImporter = false
    @State private var statusMessage: String?

    var body: some View {
        VStack(spacing: 0) {
            header
            if !selectedIDs.isEmpty {
                HStack {
                    Text("\(selectedIDs.count)개 선택")
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                    Button("해제") { selectedIDs.removeAll() }
                        .buttonStyle(ReadingSecondaryButtonStyle())
                    Button("백업") { backupSelected() }
                        .buttonStyle(ReadingPrimaryButtonStyle())
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
            content
        }
        .background(palette.background)
        .overlay(alignment: .bottomTrailing) {
            Button {
                isCreating = true
            } label: {
                Image(systemName: "plus")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(palette.isDark ? palette.onSurface : .white)
                    .frame(width: 56, height: 56)
                    .background(palette.primary, in: Circle())
                    .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
            }
            .padding(.trailing, 20)
            .padding(.bottom, PersonalNotesLayout.fabBottomPadding)
        }
        .sheet(isPresented: $showExporter) {
            PersonalNoteDocumentExporter(data: exportData, filename: personalNotesBackupFilename())
        }
        .sheet(isPresented: $showImporter) {
            PersonalNoteDocumentImporter { data in
                restoreNotes(from: data)
            }
        }
        .sheet(item: $editingNote) { note in
            PersonalNoteEditorSheet(
                title: "메모 수정",
                initialTitle: note.title,
                initialBody: note.body
            ) { title, body in
                var updated = note
                updated.title = title
                updated.body = body
                environment.updatePersonalNote(updated)
            }
            .readingTheme(environment.readingStyle.palette)
        }
        .sheet(isPresented: $isCreating) {
            PersonalNoteEditorSheet(title: "새 메모", initialTitle: "", initialBody: "") { title, body in
                _ = environment.addPersonalNote(title: title, body: body)
            }
            .readingTheme(environment.readingStyle.palette)
        }
        .overlay(alignment: .top) {
            if let statusMessage {
                Text(statusMessage)
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(palette.surfaceContainer, in: Capsule())
                    .padding(.top, 8)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("개인메모")
                .font(.title3.weight(.semibold))
                .foregroundStyle(palette.onSurface)
            Text("성경 구절과 무관하게 자유롭게 적는 메모입니다. 길게 눌러 선택한 뒤 백업할 수 있습니다.")
                .font(.caption)
                .readingSecondaryForeground()
            HStack {
                Button("복원") { showImporter = true }
                    .buttonStyle(ReadingSecondaryButtonStyle())
                Spacer()
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 8)
    }

    @ViewBuilder
    private var content: some View {
        if environment.personalNotes.isEmpty {
            VStack(spacing: 12) {
                Spacer()
                Image(systemName: "square.and.pencil")
                    .font(.system(size: 40))
                    .foregroundStyle(palette.onSurfaceVariant)
                Text("개인메모 없음")
                    .font(.headline)
                Text("오른쪽 아래 + 버튼으로 새 메모를 추가하세요.")
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .readingSecondaryForeground()
                    .padding(.horizontal, 32)
                Spacer()
            }
        } else {
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(environment.personalNotes) { note in
                        PersonalNoteSwipeRow(
                            note: note,
                            isSelected: selectedIDs.contains(note.id),
                            isRevealed: revealedNoteID == note.id,
                            onReveal: { revealedNoteID = note.id },
                            onCollapse: {
                                if revealedNoteID == note.id {
                                    revealedNoteID = nil
                                }
                            },
                            onTap: {
                                if revealedNoteID == note.id {
                                    revealedNoteID = nil
                                } else if selectedIDs.isEmpty {
                                    editingNote = note
                                } else {
                                    toggleSelection(note.id)
                                }
                            },
                            onLongPress: { toggleSelection(note.id) },
                            onEdit: {
                                revealedNoteID = nil
                                editingNote = note
                            },
                            onDelete: {
                                revealedNoteID = nil
                                environment.deletePersonalNote(id: note.id)
                                selectedIDs.remove(note.id)
                            }
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 80)
            }
        }
    }

    private func toggleSelection(_ id: String) {
        if selectedIDs.contains(id) {
            selectedIDs.remove(id)
        } else {
            selectedIDs.insert(id)
        }
    }

    private func backupSelected() {
        guard !selectedIDs.isEmpty else { return }
        do {
            exportData = try environment.exportPersonalNotes(ids: selectedIDs)
            showExporter = true
            flash("선택한 메모를 백업합니다.")
        } catch {
            flash(error.localizedDescription)
        }
    }

    private func restoreNotes(from data: Data) {
        do {
            try environment.importPersonalNotes(from: data, merge: true)
            selectedIDs.removeAll()
            flash("개인메모를 복원했습니다.")
        } catch {
            flash(error.localizedDescription)
        }
    }

    private func flash(_ message: String) {
        statusMessage = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if statusMessage == message {
                statusMessage = nil
            }
        }
    }

    private func formattedDate(_ millis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        return date.formatted(date: .abbreviated, time: .shortened)
    }

    private func personalNotesBackupFilename() -> String {
        let stamp = ISO8601DateFormatter().string(from: Date()).replacingOccurrences(of: ":", with: "-")
        return "personal-notes-\(stamp).rmb-notes.json"
    }
}

private struct PersonalNoteSwipeRow: View {
    let note: PersonalNote
    let isSelected: Bool
    let isRevealed: Bool
    let onReveal: () -> Void
    let onCollapse: () -> Void
    let onTap: () -> Void
    let onLongPress: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    @Environment(\.readingPalette) private var palette
    @State private var dragOffset: CGFloat = 0

    private var actionWidth: CGFloat { PersonalNotesLayout.swipeActionWidth }

    private var displayedOffset: CGFloat {
        min(0, max(-actionWidth, dragOffset))
    }

    var body: some View {
        ZStack(alignment: .trailing) {
            HStack(spacing: 0) {
                Button(action: onEdit) {
                    Image(systemName: "pencil")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(palette.primary.opacity(0.18))
                        .foregroundStyle(palette.primary)
                }
                .frame(width: actionWidth / 2)

                Button(action: onDelete) {
                    Image(systemName: "trash.fill")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.red)
                        .foregroundStyle(.white)
                }
                .frame(width: actionWidth / 2)
            }
            .frame(width: actionWidth)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            rowContent
                .offset(x: displayedOffset)
                .gesture(swipeGesture)
                .simultaneousGesture(
                    LongPressGesture(minimumDuration: 0.35).onEnded { _ in
                        onLongPress()
                    }
                )
                .onTapGesture { onTap() }
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .onAppear {
            dragOffset = isRevealed ? -actionWidth : 0
        }
        .onChange(of: isRevealed) { _, revealed in
            withAnimation(.easeOut(duration: 0.2)) {
                dragOffset = revealed ? -actionWidth : 0
            }
        }
    }

    private var rowContent: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(note.title.isEmpty ? "제목 없음" : note.title)
                .font(.headline)
                .foregroundStyle(palette.onSurface)
            if !note.body.isEmpty {
                Text(note.body)
                    .font(.subheadline)
                    .foregroundStyle(palette.onSurfaceVariant)
                    .lineLimit(3)
            }
            Text(formattedDate(note.updatedAtMillis))
                .font(.caption2)
                .readingSecondaryForeground()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 10)
        .padding(.horizontal, 12)
        .background(palette.surfaceContainer, in: RoundedRectangle(cornerRadius: 12))
        .overlay {
            if isSelected {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(palette.primary, lineWidth: 2)
            }
        }
        .overlay {
            if isSelected {
                RoundedRectangle(cornerRadius: 12)
                    .fill(palette.primary.opacity(0.12))
            }
        }
    }

    private var swipeGesture: some Gesture {
        DragGesture(minimumDistance: 12, coordinateSpace: .local)
            .onChanged { value in
                let base: CGFloat = isRevealed ? -actionWidth : 0
                dragOffset = min(0, max(-actionWidth, base + value.translation.width))
            }
            .onEnded { value in
                withAnimation(.easeOut(duration: 0.2)) {
                    if displayedOffset <= -actionWidth / 2 {
                        dragOffset = -actionWidth
                        onReveal()
                    } else {
                        dragOffset = 0
                        onCollapse()
                    }
                }
            }
    }

    private func formattedDate(_ millis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        return date.formatted(date: .abbreviated, time: .shortened)
    }
}
