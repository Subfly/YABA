//
//  Created by Ali Taha on 20.04.2026.
//

import SwiftData
import SwiftUI
import UIKit

/// SwiftData-driven link bookmark detail + Milkdown readable host.
struct LinkmarkDetailView: View {
    let bookmarkId: String
    let onOpenFolder: (String) -> Void
    let onOpenTag: (String) -> Void

    @Environment(\.dismiss)
    private var dismiss

    @Environment(\.colorScheme)
    private var colorScheme

    @Query
    private var bookmarks: [YabaBookmark]

    @State
    private var machine = LinkmarkDetailStateMachine()

    @State
    private var sheetTab: LinkmarkDetailSheetTab = .info
    
    @State
    private var readerCanAnnotate = false

    @State
    private var documentReloadToken = UUID()

    @State
    private var showDetailSheet = false
    
    @State
    private var showEditSheet = false

    @State
    private var showMoveSheet = false

    @State
    private var showShareURLSheet = false
    
    @State
    private var showReminderSheet = false

    @State
    private var showDeleteAlert = false

    @State
    private var reminderDraft = Date().addingTimeInterval(3600)

    @State
    private var activityItems: [Any] = []

    @State
    private var showActivitySheet = false

    @State
    private var markdownExportRequest: MarkdownExportRequest?

    @State
    private var showMarkdownExportDirectoryPicker = false

    @State
    private var annotationSheetMode: AnnotationCreationSheetMode?

    init(
        bookmarkId: String,
        onOpenFolder: @escaping (String) -> Void = { _ in },
        onOpenTag: @escaping (String) -> Void = { _ in }
    ) {
        self.bookmarkId = bookmarkId
        self.onOpenFolder = onOpenFolder
        self.onOpenTag = onOpenTag
        var d = FetchDescriptor<YabaBookmark>(
            predicate: #Predicate<YabaBookmark> { $0.bookmarkId == bookmarkId }
        )
        d.fetchLimit = 1
        _bookmarks = Query(d, animation: .smooth)
    }

    var body: some View {
        Group {
            if let bm = bookmark {
                if bm.kind == .link {
                    mainContent(for: bm)
                } else {
                    EmptyView()
                }
            } else {
                EmptyView()
            }
        }
        .navigationBarBackButtonHidden()
        .task {
            await machine.send(.onInit(bookmarkId: bookmarkId))
            if let url = bookmark?.linkDetail?.url {
                await machine.send(.onLinkSourceUrl(url))
            }
        }
        .onChange(of: bookmark?.linkDetail?.url) { _, newUrl in
            guard let newUrl else { return }
            Task { await machine.send(.onLinkSourceUrl(newUrl)) }
        }
        .sheet(isPresented: $showDetailSheet) {
            if let bm = bookmark {
                LinkmarkDetailInfoSheet(
                    bookmark: bm,
                    folderAccent: folderColor(for: bm),
                    reminderDate: machine.state.reminderDate,
                    onDeleteReminder: {
                        Task { await machine.send(.onCancelReminder) }
                    },
                    onOpenFolder: { folderId in
                        showDetailSheet = false
                        onOpenFolder(folderId)
                    },
                    onOpenTag: { tagId in
                        showDetailSheet = false
                        onOpenTag(tagId)
                    },
                    selectedTab: $sheetTab,
                    onScrollToAnnotation: { annotationId in
                        showDetailSheet = false
                        Task { await machine.send(.onScrollToAnnotation(annotationId: annotationId)) }
                    },
                    onEditAnnotation: { annotationId in
                        presentAnnotationSheetAfterClosingDetail(annotationId: annotationId)
                    },
                    onDeleteAnnotation: { annotationId in
                        showDetailSheet = false
                    }
                )
            }
        }
        .sheet(item: $annotationSheetMode) { mode in
            AnnotationCreationSheet(mode: mode) { outcome in
            }
        }
        .sheet(isPresented: $showEditSheet) {
            if let bm = bookmark {
                BookmarkFlowSheet(context: BookmarkFlowContext.edit(bookmarkId: bm.bookmarkId))
            }
        }
        .sheet(isPresented: $showMoveSheet) {
            if let bm = bookmark {
                NavigationStack {
                    SelectFolderContent(
                        mode: .bookmarksMove,
                        contextFolderId: bm.folder?.folderId,
                        contextBookmarkIds: [bm.bookmarkId],
                        onPick: { target in
                            if let target {
                                AllBookmarksManager.queueMoveBookmarksToFolder(
                                    bookmarkIds: [bm.bookmarkId],
                                    targetFolderId: target
                                )
                            }
                            showMoveSheet = false
                        }
                    )
                }
            }
        }
        .sheet(isPresented: $showShareURLSheet) {
            if let urlStr = bookmark?.linkDetail?.url, let u = URL(string: urlStr) {
                ShareSheet(bookmarkLink: u)
            }
        }
        .sheet(isPresented: $showActivitySheet) {
            ActivityItemsShareSheet(items: activityItems)
        }
        .sheet(isPresented: $showMarkdownExportDirectoryPicker) {
            MarkdownExportDirectoryPicker { url in
                Task { @MainActor in
                    finalizeMarkdownExport(selectedDirectory: url)
                }
            }
        }
        .sheet(isPresented: $showReminderSheet) {
            NavigationStack {
                DatePicker(
                    "Setup Reminder Picker Title",
                    selection: $reminderDraft,
                    in: Date()...,
                    displayedComponents: [.date, .hourAndMinute]
                )
                .datePickerStyle(.graphical)
                .padding()
                .navigationTitle("Setup Reminder Title")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { showReminderSheet = false }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") {
                            Task {
                                await machine.send(.onRequestNotificationPermission)
                                await machine.send(
                                    .onScheduleReminder(
                                        fireAt: reminderDraft,
                                        titleKey: "Reminder Default Title",
                                        messageKey: "Reminder Default Body"
                                    )
                                )
                            }
                            showReminderSheet = false
                        }
                    }
                }
            }
        }
        .alert("Delete Bookmark Title", isPresented: $showDeleteAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                Task {
                    await machine.send(.onDeleteBookmark(bookmarkId: bookmarkId))
                    dismiss()
                }
            }
        } message: {
            if let bm = bookmark {
                Text("Delete Content Message \(bm.label)")
            }
        }
    }

    private var bookmark: YabaBookmark? { bookmarks.first }

    @ViewBuilder
    private func mainContent(for bm: YabaBookmark) -> some View {
        let hasReadable = linkHasReadableContent(bm)
        let folderTint = folderColor(for: bm)
        Group {
            if !hasReadable {
                LinkmarkNoReadableVersionView(accent: folderTint)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ZStack(alignment: .top) {
                    linkmarkReaderBackground(readerTheme: machine.state.readerTheme)
                    MarkdownPreview(
                        markdown: readableBodyString(for: bm),
                        configuration: MarkdownPreviewConfiguration(
                            showFrontMatter: true,
                            showLinkReferenceBlocks: true,
                            useWebViewForHtmlBlocks: true,
                            assetRegistry: imageAssetRegistry(for: bm),
                            baseURLForRelativeLinks: linkSourceURL(for: bm)
                        ),
                        theme: markdownThemeTokens(
                            state: machine.state,
                            systemColorScheme: colorScheme
                        )
                    )
                    .id("\(bm.bookmarkId)-\(documentReloadToken)")
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)

                    VStack {
                        Spacer()
                        LinkmarkReaderFloatingToolbar(
                            folderAccent: folderTint,
                            isVisible: readerCanAnnotate,
                            canAnnotate: readerCanAnnotate,
                            readerTheme: machine.state.readerTheme,
                            readerFontSize: machine.state.readerFontSize,
                            readerLineHeight: machine.state.readerLineHeight,
                            onSelectTheme: { r in Task { await machine.send(.onSetReaderTheme(r)) } },
                            onSelectFontSize: { f in Task { await machine.send(.onSetReaderFontSize(f)) } },
                            onSelectLineHeight: { lh in Task { await machine.send(.onSetReaderLineHeight(lh)) } },
                            onStickyNote: {
                                openAnnotationCreator()
                            }
                        )
                        .safeAreaPadding([.bottom])
                        .padding(.bottom, 24)
                    }
                }
                .environment(
                    \.colorScheme,
                    effectiveReaderColorScheme(readerTheme: machine.state.readerTheme)
                )
                .ignoresSafeArea()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    dismiss()
                } label: {
                    homeToolbarIcon("arrow-left-01")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showDetailSheet = true
                } label: {
                    homeToolbarIcon("information-circle")
                }
            }
            if #available(iOS 26, *) {
                ToolbarSpacer(.fixed, placement: .topBarTrailing)
            }
            ToolbarItem(placement: .topBarTrailing) {
                overflowMenu(for: bm)
            }
        }
        .tint(folderTint)
    }

    private func folderColor(for bm: YabaBookmark) -> Color {
        bm.folder?.color.getUIColor() ?? .accentColor
    }

    private func linkHasReadableContent(_ bm: YabaBookmark) -> Bool {
        let md = bm.linkDetail?.markdown ?? ""
        return !md.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func readableBodyString(for bm: YabaBookmark) -> String {
        bm.linkDetail?.markdown ?? ""
    }

    @ViewBuilder
    private func linkmarkReaderBackground(readerTheme: ReaderTheme) -> some View {
        if readerTheme == .sepia {
            Color(red: 0.98, green: 0.95, blue: 0.88)
        } else {
            Color(.systemBackground)
        }
    }

    private func effectiveReaderColorScheme(readerTheme: ReaderTheme) -> ColorScheme {
        switch readerTheme {
        case .light, .sepia: return .light
        case .dark: return .dark
        case .system: return colorScheme
        }
    }

    private func linkSourceURL(for bm: YabaBookmark) -> URL? {
        guard let s = bm.linkDetail?.url.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { return nil }
        return URL(string: s)
    }

    private func imageAssetRegistry(for bm: YabaBookmark) -> MarkdownImageAssetRegistry {
        guard let link = bm.linkDetail else { return MarkdownImageAssetRegistry() }
        var map: [String: Data] = [:]
        for a in link.inlineAssets {
            guard let bytes = a.bytes, !bytes.isEmpty else { continue }
            map[a.assetId] = bytes
        }
        return MarkdownImageAssetRegistry(assetsById: map)
    }

    private func markdownThemeTokens(
        state: LinkmarkDetailUIState,
        systemColorScheme: ColorScheme
    ) -> MarkdownThemeTokens {
        let effective: ColorScheme = {
            switch state.readerTheme {
            case .light: return .light
            case .dark: return .dark
            case .system: return systemColorScheme
            case .sepia: return .light
            }
        }()
        var tokens = MarkdownThemeTokens.standard(colorScheme: effective)
        if state.readerTheme == .sepia {
            tokens.codeBackground = Color(red: 0.94, green: 0.9, blue: 0.82)
            tokens.tableHeader = Color(red: 0.96, green: 0.93, blue: 0.86)
        }
        let baseSize: CGFloat = switch state.readerFontSize {
        case .small: 15
        case .medium: 17
        case .large: 20
        }
        tokens.body = .system(size: baseSize)
        tokens.monospaced = .system(size: baseSize, design: .monospaced)
        tokens.largeHeading = .system(size: baseSize + 9, weight: .bold)
        return tokens
    }

    private func annotationsJson(for bm: YabaBookmark) -> String {
        let ann = bm.annotations.filter { $0.type == .readable }
        return AnnotationRenderingPayloadBuilder.readableJSON(from: ann)
    }

    private func openAnnotationCreator() {
        guard let bm = bookmark else { return }
        guard linkHasReadableContent(bm) else { return }
        Task { @MainActor in
            // WebKit selection and host metrics can be one frame apart; retry briefly before showing an error.
            var draft: ReadableSelectionDraft?
            for _ in 0 ..< 5 {
                
                if draft != nil { break }
                try? await Task.sleep(nanoseconds: 80_000_000)
            }
            if let draft {
                annotationSheetMode = .create(draft)
            } else {
                CoreToastManager.shared.show(
                    message: LocalizedStringKey("Annotation Selection Required Message"),
                    iconType: .error,
                    duration: .short
                )
            }
        }
    }

    private func openAnnotationEditor(annotationId: String) {
        guard let ann = bookmark?.annotations.first(where: { $0.annotationId == annotationId }) else { return }
        annotationSheetMode = .edit(ann)
    }

    private func presentAnnotationSheetAfterClosingDetail(annotationId: String) {
        guard let ann = bookmark?.annotations.first(where: { $0.annotationId == annotationId }) else { return }
        showDetailSheet = false
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 200_000_000)
            annotationSheetMode = .edit(ann)
        }
    }

    @ViewBuilder
    private func overflowMenu(for bm: YabaBookmark) -> some View {
        Menu {
            if let urlStr = bm.linkDetail?.url, let u = URL(string: urlStr) {
                Button {
                    UIApplication.shared.open(u)
                } label: {
                    overflowMenuItemLabel("Bookmark Detail Open Link Action", icon: "link-04")
                }
                .tint(YabaColor.green.getUIColor())
            }
            Button {
                showEditSheet = true
            } label: {
                overflowMenuItemLabel("Edit", icon: "edit-02")
            }
            .tint(YabaColor.orange.getUIColor())
            Button {
                showMoveSheet = true
            } label: {
                overflowMenuItemLabel("Move", icon: "arrow-move-up-right")
            }
            .tint(YabaColor.teal.getUIColor())
            Button {
                AllBookmarksManager.queueToggleBookmarkPinned(bookmarkId: bm.bookmarkId)
            } label: {
                overflowMenuItemLabel(
                    bm.isPinned ? "Bookmark Detail Unpin Action" : "Bookmark Detail Pin Action",
                    icon: bm.isPinned ? "pin" : "pin-off"
                )
            }
            .tint(YabaColor.yellow.getUIColor())
            Menu {
                Button {
                    Task { @MainActor in
                        startMarkdownExport(markdown: readableBodyString(for: bm), bookmark: bm)
                    }
                } label: {
                    overflowMenuItemLabel(
                        "Bookmark Detail Export Format Markdown Title",
                        icon: "document-attachment"
                    )
                }
                .tint(YabaColor.gray.getUIColor())
                Button {
                    
                } label: {
                    overflowMenuItemLabel(
                        "Bookmark Detail Export Format PDF Title",
                        icon: "pdf-02"
                    )
                }
                .tint(YabaColor.red.getUIColor())
            } label: {
                overflowMenuItemLabel("Bookmark Detail Export Menu Title", icon: "download-01")
            }
            .tint(YabaColor.blue.getUIColor())
            if machine.state.reminderDate == nil {
                Button {
                    showReminderSheet = true
                } label: {
                    overflowMenuItemLabel("Remind Me", icon: "notification-01")
                }
                .tint(YabaColor.yellow.getUIColor())
            }
            Button {
                showShareURLSheet = true
            } label: {
                overflowMenuItemLabel("Share", icon: "share-03")
            }
            .tint(YabaColor.indigo.getUIColor())
            Divider()
            if machine.state.reminderDate != nil {
                Button {
                    Task { await machine.send(.onCancelReminder) }
                } label: {
                    overflowMenuItemLabel(
                        "Bookmark Detail Cancel Reminder Action",
                        icon: "notification-off-03"
                    )
                }
                .tint(YabaColor.red.getUIColor())
            }
            Button {
                showDeleteAlert = true
            } label: {
                overflowMenuItemLabel("Delete", icon: "delete-02")
            }
            .tint(YabaColor.red.getUIColor())
        } label: {
            homeToolbarIcon("more-horizontal-circle-02")
        }
    }

    /// Same template size as `HomeCollectionView` section headers and `LinkmarkReaderFloatingToolbar` glyphs (22×22).
    @ViewBuilder
    private func homeToolbarIcon(_ bundleKey: String) -> some View {
        YabaIconView(bundleKey: bundleKey)
            .frame(width: 22, height: 22)
    }

    /// Plain label; color the row with `.tint(...)` on the `Button` or `Menu` (same pattern as `FolderDetailView` overflow actions).
    @ViewBuilder
    private func overflowMenuItemLabel(_ key: LocalizedStringKey, icon: String) -> some View {
        Label {
            Text(key)
        } icon: {
            YabaIconView(bundleKey: icon)
                .scaledToFit()
                .frame(width: 20, height: 20)
        }
    }

    private func startMarkdownExport(markdown: String, bookmark: YabaBookmark?) {
        guard let bookmark else { return }
        let trimmed = markdown.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            CoreToastManager.shared.show(
                message: LocalizedStringKey("Bookmark Detail Markdown Export Failed Message"),
                iconType: .error,
                duration: .short
            )
            return
        }
        let inlineSources: [MarkdownExportInlineSource] = (bookmark.linkDetail?.inlineAssets ?? []).compactMap { item in
            guard let bytes = item.bytes, !bytes.isEmpty else { return nil }
            return MarkdownExportInlineSource(assetId: item.assetId, pathExtension: item.pathExtension, bytes: bytes)
        }
        markdownExportRequest = MarkdownExportRequest(
            markdown: trimmed + "\n",
            baseFolderName: MarkdownExportSupport.sanitizeBaseFolderName(bookmark.label),
            assets: MarkdownExportSupport.exportAssets(from: inlineSources)
        )
        showMarkdownExportDirectoryPicker = true
    }

    private func finalizeMarkdownExport(selectedDirectory: URL?) {
        defer { markdownExportRequest = nil }
        guard let selectedDirectory, let request = markdownExportRequest else { return }
        let didWrite = MarkdownExportSupport.writeBundle(request, into: selectedDirectory)
        if !didWrite {
            CoreToastManager.shared.show(
                message: LocalizedStringKey("Bookmark Detail Markdown Export Failed Message"),
                iconType: .error,
                duration: .short
            )
        }
    }
}

/// Same CUV pattern as the old reader-not-available empty state (legacy `ReaderView` was removed during the Darwin rebuild).
private struct LinkmarkNoReadableVersionView: View {
    let accent: Color

    var body: some View {
        ContentUnavailableView {
            Label {
                Text("Reader Not Available Title")
                    .padding(.bottom)
            } icon: {
                YabaIconView(bundleKey: "cancel-square")
                    .scaledToFit()
                    .frame(width: 52, height: 52)
                    .foregroundStyle(accent)
                    .padding(.top)
            }
        } description: {
            Text("Reader Not Available Description")
                .padding(
                    .horizontal,
                    UIDevice.current.userInterfaceIdiom == .pad ? 52 : 0
                )
        }
    }
}
