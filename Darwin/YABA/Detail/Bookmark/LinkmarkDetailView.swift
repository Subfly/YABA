//
//  Created by Ali Taha on 20.04.2026.
//

import SwiftData
import SwiftUI
import UIKit

/// SwiftData-driven link bookmark detail + Milkdown readable host.
struct LinkmarkDetailView: View {
    let bookmarkId: String

    @Environment(\.dismiss)
    private var dismiss

    @Environment(\.colorScheme)
    private var colorScheme

    @Query
    private var bookmarks: [YabaBookmark]

    @State
    private var machine = LinkmarkDetailStateMachine()

    @State
    private var webDriver = LinkmarkWebDriver()

    @State
    private var showDetailSheet = false

    @State
    private var sheetTab: LinkmarkDetailSheetTab = .info

    @State
    private var readerChromeVisible = true

    @State
    private var readerCanAnnotate = false

    @State
    private var documentReloadToken = UUID()

    @State
    private var pendingWebToc: LinkmarkWebTocNavigation?

    @State
    private var measuredTopInset: CGFloat = 108

    @State
    private var showEditSheet = false

    @State
    private var showMoveSheet = false

    @State
    private var showShareURLSheet = false

    @State
    private var showDeleteAlert = false

    @State
    private var showReminderSheet = false

    @State
    private var reminderDraft = Date().addingTimeInterval(3600)

    @State
    private var activityItems: [Any] = []

    @State
    private var showActivitySheet = false

    init(bookmarkId: String) {
        self.bookmarkId = bookmarkId
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
        .onChange(of: machine.state.pendingTocNavigationId) { _, newId in
            guard let newId else { return }
            pendingWebToc = LinkmarkWebTocNavigation(
                id: newId,
                extrasJson: machine.state.pendingTocNavigationExtrasJson
            )
        }
        .sheet(isPresented: $showDetailSheet) {
            if let bm = bookmark {
                LinkmarkDetailInfoSheet(
                    bookmark: bm,
                    toc: decodedToc,
                    folderAccent: folderColor(for: bm),
                    selectedTab: $sheetTab,
                    sortedVersions: sortedVersions(bm),
                    selectedVersionId: machine.state.selectedReadableVersionId,
                    onSelectVersion: { id in
                        Task { await machine.send(.onSelectReadableVersion(versionId: id)) }
                        documentReloadToken = UUID()
                    },
                    onDeleteVersion: { id in
                        Task { await machine.send(.onDeleteReadableVersion(versionId: id)) }
                    },
                    onTocItemTap: { item in
                        showDetailSheet = false
                        Task { await machine.send(.onNavigateToTocItem(id: item.id, extrasJson: item.extrasJson)) }
                    }
                )
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
        let folderTint = folderColor(for: bm)
        ZStack(alignment: .top) {
            AnimatedGradient(color: folderTint)

            LinkmarkReadableWebView(
                webDriver: webDriver,
                markdown: markdownString(for: bm),
                assetsBaseUrl: ReadableViewerAssets.assetsBaseURLForYabaAssetScheme,
                annotationsJson: annotationsJson(for: bm),
                readerPreferences: ReaderPreferences(
                    theme: machine.state.readerTheme,
                    fontSize: machine.state.readerFontSize,
                    lineHeight: machine.state.readerLineHeight
                ),
                topChromeInsetPoints: measuredTopInset,
                colorScheme: colorScheme,
                documentReloadToken: documentReloadToken,
                inlineAssets: inlineAssetTuples(for: bm),
                tocNavigate: pendingWebToc,
                scrollToAnnotationId: machine.state.scrollToAnnotationId,
                onHostEvent: handleHostEvent(_:),
                onScrollDirection: { dir in
                    switch dir {
                    case .down: readerChromeVisible = false
                    case .up: readerChromeVisible = true
                    }
                },
                onBridgeReady: {},
                onTocNavigationConsumed: {
                    pendingWebToc = nil
                    Task { await machine.send(.onClearTocNavigation) }
                },
                onScrollToAnnotationConsumed: {
                    Task { await machine.send(.onClearScrollToAnnotation) }
                }
            )
            .ignoresSafeArea()

            VStack {
                Spacer()
                LinkmarkReaderFloatingToolbar(
                    folderAccent: folderTint,
                    isVisible: readerChromeVisible || readerCanAnnotate,
                    canAnnotate: readerCanAnnotate,
                    readerTheme: machine.state.readerTheme,
                    readerFontSize: machine.state.readerFontSize,
                    readerLineHeight: machine.state.readerLineHeight,
                    onSelectTheme: { r in Task { await machine.send(.onSetReaderTheme(r)) } },
                    onSelectFontSize: { f in Task { await machine.send(.onSetReaderFontSize(f)) } },
                    onSelectLineHeight: { lh in Task { await machine.send(.onSetReaderLineHeight(lh)) } },
                    onStickyNote: {
                        // Annotation creation flow deferred.
                    }
                )
                .padding(.bottom, 24)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    dismiss()
                } label: {
                    YabaIconView(bundleKey: "arrow-left-01")
                }
                .buttonStyle(.plain)
            }
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button {
                    showDetailSheet = true
                } label: {
                    YabaIconView(bundleKey: "information-circle")
                }
                .buttonStyle(.plain)
                overflowMenu(for: bm)
            }
        }
        .background {
            GeometryReader { proxy in
                Color.clear
                    .onAppear { updateReaderTopInset(proxy) }
                    .onChange(of: proxy.size.height) { _, _ in updateReaderTopInset(proxy) }
                    .onChange(of: proxy.size.width) { _, _ in updateReaderTopInset(proxy) }
            }
            .ignoresSafeArea()
        }
    }

    private func updateReaderTopInset(_ proxy: GeometryProxy) {
        // Content is laid out below the nav bar, so `proxy.safeAreaInsets.top` is often 0.
        // Reader WebView ignores safe area; use the window’s top inset (status bar / notch) + bar height.
        let windowTop: CGFloat
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = scene.windows.first(where: { $0.isKeyWindow }) ?? scene.windows.first {
            windowTop = window.safeAreaInsets.top
        } else {
            windowTop = proxy.safeAreaInsets.top
        }
        let navigationBarHeight: CGFloat = 44
        measuredTopInset = windowTop + navigationBarHeight + 12
    }

    private var decodedToc: Toc? {
        guard let j = machine.state.tocJson, let d = j.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(Toc.self, from: d)
    }

    private func folderColor(for bm: YabaBookmark) -> Color {
        bm.folder?.color.getUIColor() ?? .accentColor
    }

    private func sortedVersions(_ bm: YabaBookmark) -> [ReadableVersionModel] {
        bm.readableVersions.sorted { $0.createdAt > $1.createdAt }
    }

    private func currentVersion(_ bm: YabaBookmark) -> ReadableVersionModel? {
        let sorted = sortedVersions(bm)
        guard !sorted.isEmpty else { return nil }
        let sel = machine.state.selectedReadableVersionId
        if let sel, let v = sorted.first(where: { $0.readableVersionId == sel }) {
            return v
        }
        return sorted.first
    }

    private func markdownString(for bm: YabaBookmark) -> String {
        guard let data = currentVersion(bm)?.payload?.documentJson,
              let s = String(data: data, encoding: .utf8)
        else {
            return ""
        }
        return s
    }

    private func inlineAssetTuples(for bm: YabaBookmark) -> [(assetId: String, bytes: Data)] {
        guard let v = currentVersion(bm) else { return [] }
        return v.inlineAssets.map { ($0.assetId, $0.bytes ?? Data()) }
    }

    private func annotationsJson(for bm: YabaBookmark) -> String {
        guard let v = currentVersion(bm) else { return "[]" }
        let ann = bm.annotations.filter { $0.readableVersion?.readableVersionId == v.readableVersionId }
        let payload: [[String: String]] = ann.map {
            [
                "id": $0.annotationId,
                "colorRole": annotationColorToken(YabaColor.from(colorCode: $0.colorRoleRaw)),
            ]
        }
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let s = String(data: data, encoding: .utf8)
        else {
            return "[]"
        }
        return s
    }

    private func annotationColorToken(_ y: YabaColor) -> String {
        switch y {
        case .none, .yellow: return "YELLOW"
        case .blue: return "BLUE"
        case .brown: return "BROWN"
        case .cyan: return "CYAN"
        case .gray: return "GRAY"
        case .green: return "GREEN"
        case .indigo: return "INDIGO"
        case .mint: return "MINT"
        case .orange: return "ORANGE"
        case .pink: return "PINK"
        case .purple: return "PURPLE"
        case .red: return "RED"
        case .teal: return "TEAL"
        }
    }

    private func handleHostEvent(_ event: WebHostEvent) {
        switch event {
        case let .readerMetrics(m):
            readerCanAnnotate = m.canCreateAnnotation
        default:
            let mapped = WebHostLinkmarkIntegration.linkmarkEvents(from: event)
            for e in mapped {
                Task { await machine.send(e) }
            }
        }
    }

    @ViewBuilder
    private func overflowMenu(for bm: YabaBookmark) -> some View {
        Menu {
            if let urlStr = bm.linkDetail?.url, let u = URL(string: urlStr) {
                Button {
                    UIApplication.shared.open(u)
                } label: {
                    overflowItemLabel("Bookmark Detail Open Link Action", icon: "link-02")
                }
            }
            Button {
                showEditSheet = true
            } label: {
                overflowItemLabel("Edit", icon: "edit-02")
            }
            Button {
                showMoveSheet = true
            } label: {
                overflowItemLabel("Move", icon: "folder-01")
            }
            Button {
                AllBookmarksManager.queueToggleBookmarkPinned(bookmarkId: bm.bookmarkId)
            } label: {
                overflowItemLabel(
                    bm.isPinned ? "Bookmark Detail Unpin Action" : "Bookmark Detail Pin Action",
                    icon: bm.isPinned ? "pin-off" : "pin"
                )
            }
            Menu("Bookmark Detail Export Menu Title") {
                Button {
                    Task { @MainActor in
                        let md = await webDriver.exportMarkdown()
                        exportMarkdownShare(markdown: md)
                    }
                } label: {
                    overflowItemLabel("Bookmark Detail Export Format Markdown Title", icon: "text")
                }
                Button {
                    startPdfExport()
                } label: {
                    overflowItemLabel("Bookmark Detail Export Format PDF Title", icon: "paragraph")
                }
            }
            Menu("Bookmark Detail Update Menu Title") {
                Button {
                    Task { await machine.send(.onUpdateLinkMetadataRequested) }
                } label: {
                    overflowItemLabel("Bookmark Creation Metadata Section Title", icon: "database-01")
                }
                Button {
                    Task { await machine.send(.onUpdateReadableRequested) }
                } label: {
                    overflowItemLabel("Bookmark Detail Update Readable Version Action", icon: "refresh")
                }
            }
            if machine.state.reminderDate != nil {
                Button(role: .destructive) {
                    Task { await machine.send(.onCancelReminder) }
                } label: {
                    overflowItemLabel("Bookmark Detail Cancel Reminder Action", icon: "notification-01")
                }
            } else {
                Button {
                    showReminderSheet = true
                } label: {
                    overflowItemLabel("Remind Me", icon: "notification-01")
                }
            }
            Button {
                showShareURLSheet = true
            } label: {
                overflowItemLabel("Share", icon: "share-03")
            }
            Button(role: .destructive) {
                showDeleteAlert = true
            } label: {
                overflowItemLabel("Delete", icon: "delete-02")
            }
        } label: {
            YabaIconView(bundleKey: "more-horizontal-circle-02")
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
        }
    }

    @ViewBuilder
    private func overflowItemLabel(_ key: LocalizedStringKey, icon: String) -> some View {
        Label {
            Text(key)
        } icon: {
            YabaIconView(bundleKey: icon)
                .scaledToFit()
                .frame(width: 20, height: 20)
        }
    }

    private func exportMarkdownShare(markdown: String) {
        let tmp = FileManager.default.temporaryDirectory.appendingPathComponent("YABA-export.md")
        do {
            try markdown.data(using: .utf8)?.write(to: tmp)
            activityItems = [tmp]
            showActivitySheet = true
        } catch {
            CoreToastManager.shared.show(
                message: LocalizedStringKey("Bookmark Detail Markdown Export Failed Message"),
                iconType: .error,
                duration: .short
            )
        }
    }

    private func startPdfExport() {
        let jobId = UUID().uuidString
        EditorPdfExportJobRegistry.shared.register(jobId: jobId) { result in
            Task { @MainActor in
                switch result {
                case let .success(b64):
                    guard let data = Data(base64Encoded: b64) else { return }
                    let tmp = FileManager.default.temporaryDirectory.appendingPathComponent("YABA-export.pdf")
                    do {
                        try data.write(to: tmp)
                        activityItems = [tmp]
                        showActivitySheet = true
                    } catch {
                        CoreToastManager.shared.show(
                            message: LocalizedStringKey("Bookmark Detail PDF Write Failed Message"),
                            iconType: .error,
                            duration: .short
                        )
                    }
                case let .failure(err):
                    CoreToastManager.shared.show(
                        message: LocalizedStringKey(stringLiteral: err.localizedDescription),
                        iconType: .error,
                        duration: .short
                    )
                }
            }
        }
        Task {
            await webDriver.startPdfExportJob(jobId: jobId)
        }
    }
}

// MARK: - Share sheet for arbitrary items

private struct ActivityItemsShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let vc = UIActivityViewController(activityItems: items, applicationActivities: nil)
        vc.modalPresentationStyle = .formSheet
        return vc
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
