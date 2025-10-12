//
//  BookmarkDetail.swift
//  YABA
//
//  Created by Ali Taha on 29.04.2025.
//

import SwiftUI
import TipKit

struct GeneralBookmarkDetail: View {
    @Environment(\.deepLinkManager)
    private var deepLinkManager
    
    @Environment(\.appState)
    private var appState
    
    @Environment(\.modelContext)
    private var modelContext
    
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    let onDeleteBookmarkCallback: (YabaBookmark) -> Void
    
    var body: some View {
        BookmarkDetail(
            bookmark: appState.selectedBookmark,
            onCollectionNavigationCallback: onCollectionNavigationCallback,
            onDeleteBookmarkCallback: { bookmark in
                appState.selectedBookmark = nil
                onDeleteBookmarkCallback(bookmark)
            }
        )
        .onChange(of: deepLinkManager.openBookmarkRequest) { oldValue, newValue in
            if oldValue == nil {
                if let newRequest = newValue {
                    let id = newRequest.bookmarkId
                    if let bookmarks = try? modelContext.fetch(
                        .init(predicate: #Predicate<YabaBookmark> { $0.bookmarkId ==  id })
                    ), let bookmark = bookmarks.first {
                        appState.selectedBookmark = bookmark
                    }
                    deepLinkManager.onHandleDeeplink()
                }
            }
        }
    }
}

struct MobileBookmarkDetail: View {
    let bookmark: YabaBookmark?
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    let onDeleteBookmarkCallback: (YabaBookmark) -> Void
    
    var body: some View {
        BookmarkDetail(
            bookmark: bookmark,
            onCollectionNavigationCallback: onCollectionNavigationCallback,
            onDeleteBookmarkCallback: onDeleteBookmarkCallback
        )
    }
}

private struct BookmarkDetail: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var state: BookmarkDetailState
    
    let bookmark: YabaBookmark?
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    let onDeleteBookmarkCallback: (YabaBookmark) -> Void
    
    init(
        bookmark: YabaBookmark?,
        onCollectionNavigationCallback: @escaping (YabaCollection) -> Void,
        onDeleteBookmarkCallback: @escaping (YabaBookmark) -> Void
    ) {
        self.state = .init(with: bookmark)
        self.bookmark = bookmark
        self.onCollectionNavigationCallback = onCollectionNavigationCallback
        self.onDeleteBookmarkCallback = onDeleteBookmarkCallback
    }
    
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: state.meshColor)
            
            if let bookmark {
                #if targetEnvironment(macCatalyst)
                GeometryReader { proxy in
                    MainContent(
                        bookmark: bookmark,
                        reminderDate: state.reminderDate,
                        folder: state.folder,
                        tags: state.tags,
                        mode: state.currentMode,
                        isLoading: state.isLoading,
                        onClickOpenLink: {
                            state.onClickOpenLink(using: bookmark)
                        },
                        onDeleteNotification: {
                            state.onRemoveReminder(from: bookmark)
                        },
                        onCollectionNavigationCallback: onCollectionNavigationCallback
                    )
                    .padding(.horizontal, proxy.size.width * 0.1)
                    .scrollIndicators(.hidden)
                }
                #else
                MainContent(
                    bookmark: bookmark,
                    reminderDate: state.reminderDate,
                    folder: state.folder,
                    tags: state.tags,
                    mode: state.currentMode,
                    isLoading: state.isLoading,
                    onClickOpenLink: {
                        state.onClickOpenLink(using: bookmark)
                    },
                    onDeleteNotification: {
                        state.onRemoveReminder(from: bookmark)
                    },
                    onCollectionNavigationCallback: onCollectionNavigationCallback
                )
                #endif
            } else {
                ContentUnavailableView {
                    Label {
                        Text("YABA")
                    } icon: {
                        YabaIconView(bundleKey: "bookmark-02")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("YABA Description")
                }
            }
        }
        .navigationTitle(
            bookmark != nil
            ? LocalizedStringKey("Bookmark Detail Title")
            : ""
        )
        .refreshable {
            if let bookmark {
                Task {
                    await state.refetchData(with: bookmark, using: modelContext)
                }
            }
        }
        .toolbar {
            if UIDevice.current.userInterfaceIdiom == .phone {
                ToolbarItem(placement: .navigation) {
                    Button {
                        dismiss()
                    } label: {
                        YabaIconView(bundleKey: "arrow-left-01")
                    }
                }
            }
            #if !targetEnvironment(macCatalyst)
            if bookmark != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        state.changeMode()
                    } label: {
                        YabaIconView(bundleKey: "text-font")
                    }
                }
            }
            #endif
            if bookmark != nil {
                #if targetEnvironment(macCatalyst)
                ToolbarItem(placement: .topBarTrailing) {
                    MacOSHoverableToolbarIcon(
                        bundleKey: "text-font",
                        tooltipKey: "Tooltip Title Reader",
                        onPressed: {
                            state.changeMode()
                        }
                    )
                    .tint(.green)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    MacOSHoverableToolbarIcon(
                        bundleKey: "edit-02",
                        tooltipKey: "Edit",
                        onPressed: {
                            state.shouldShowEditBookmarkSheet = true
                        }
                    )
                    .tint(.orange)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    MacOSHoverableToolbarIcon(
                        bundleKey: "notification-01",
                        tooltipKey: "Tooltip Title Notification",
                        onPressed: {
                            state.shouldShowTimePicker = true
                        }
                    )
                    .tint(.yellow)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    MacOSHoverableToolbarIcon(
                        bundleKey: "refresh",
                        tooltipKey: "Refresh",
                        onPressed: {
                            if let bookmark {
                                Task {
                                    await state.refetchData(with: bookmark, using: modelContext)
                                }
                            }
                        }
                    )
                    .tint(.blue)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    MacOSHoverableToolbarIcon(
                        bundleKey: "share-03",
                        tooltipKey: "Share",
                        onPressed: {
                            state.shouldShowShareDialog = true
                        }
                    )
                    .tint(.indigo)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    MacOSHoverableToolbarIcon(
                        bundleKey: "delete-02",
                        tooltipKey: "Delete",
                        onPressed: {
                            state.shouldShowDeleteDialog = true
                        }
                    )
                    .tint(.red)
                }
                #else
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button {
                            state.shouldShowEditBookmarkSheet = true
                        } label: {
                            Label {
                                Text("Edit")
                            } icon: {
                                YabaIconView(bundleKey: "edit-02")
                                    .scaledToFit()
                            }
                        }.tint(.orange)
                        Button {
                            state.shouldShowTimePicker = true
                        } label: {
                            Label {
                                Text("Remind Me")
                            } icon: {
                                YabaIconView(bundleKey: "notification-01")
                                    .scaledToFit()
                            }
                        }.tint(.yellow)
                        Button {
                            if let bookmark {
                                Task {
                                    await state.refetchData(with: bookmark, using: modelContext)
                                }
                            }
                        } label: {
                            Label {
                                Text("Refresh")
                            } icon: {
                                YabaIconView(bundleKey: "refresh")
                                    .scaledToFit()
                            }
                        }.tint(.blue)
                        Button {
                            state.shouldShowShareDialog = true
                        } label: {
                            Label {
                                Text("Share")
                            } icon: {
                                YabaIconView(bundleKey: "share-03")
                                    .scaledToFit()
                            }
                        }.tint(.indigo)
                        Divider()
                        Button(role: .destructive) {
                            state.shouldShowDeleteDialog = true
                        } label: {
                            Label {
                                Text("Delete")
                            } icon: {
                                YabaIconView(bundleKey: "delete-02")
                                    .scaledToFit()
                            }
                        }.tint(.red)
                    } label: {
                        YabaIconView(bundleKey: "more-horizontal-circle-02")
                            .scaledToFit()
                    }
                }
                #endif
            }
        }
        .alert(
            LocalizedStringKey("Delete Bookmark Title"),
            isPresented: $state.shouldShowDeleteDialog,
        ) {
            alertActionItems
        } message: {
            if let bookmark {
                Text("Delete Content Message \(bookmark.label)")
            }
        }
        .sheet(isPresented: $state.shouldShowEditBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: bookmark,
                collectionToFill: nil,
                link: nil,
                onExitRequested: {}
            )
        }
        .sheet(isPresented: $state.shouldShowShareDialog) {
            if let bookmark,
               let link = URL(string: bookmark.link) {
                ShareSheet(bookmarkLink: link)
                    .presentationDetents([.medium])
                    .presentationDragIndicator(.visible)
            }
        }
        .sheet(isPresented: $state.shouldShowTimePicker) {
            setupReminderContent
        }
        .toast(
            state: state.toastManager.toastState,
            isShowing: state.toastManager.isShowing,
            onDismiss: {
                state.toastManager.hide()
            }
        )
        .tint(state.meshColor)
        .onChange(of: bookmark) { _, newValue in
            state.retriggerUIRefresh(with: bookmark)
        }
        .onChange(of: bookmark?.collections) { _, _ in
            state.retriggerUIRefresh(with: bookmark)
        }
    }
    
    @ViewBuilder
    private var alertActionItems: some View {
        Button(role: .cancel) {
            state.shouldShowDeleteDialog = false
        } label: {
            Text("Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                if let bookmark {
                    UNUserNotificationCenter.current().removePendingNotificationRequests(
                        withIdentifiers: [bookmark.bookmarkId]
                    )
                    
                    try? YabaDataLogger.shared.logBookmarkDelete(
                        id: bookmark.bookmarkId,
                        shouldSave: false
                    )
                    
                    modelContext.delete(bookmark)
                    try? modelContext.save()
                    state.shouldShowDeleteDialog = false
                    onDeleteBookmarkCallback(bookmark)
                }
            }
        } label: {
            Text("Delete")
        }
    }
    
    @ViewBuilder
    private var setupReminderContent: some View {
        NavigationView {
            DatePicker(
                "Setup Reminder Picker Title",
                selection: .init(
                    get: { state.selectedReminderDate ?? Date.now },
                    set: { state.selectedReminderDate = $0 }
                ),
                in: Date.now...,
                displayedComponents: [.date, .hourAndMinute]
            )
            .datePickerStyle(GraphicalDatePickerStyle())
            .padding(.horizontal)
            .navigationTitle("Setup Reminder Title")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        state.shouldShowTimePicker = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        state.onNotificationPermissionRequested(
                            onSuccessCalback: {
                                if let bookmark {
                                    state.addRemindMe(to: bookmark)
                                }
                                state.shouldShowTimePicker = false
                            },
                            onDeclineCallback: {
                                state.shouldShowTimePicker = false
                            }
                        )
                    }.disabled(state.selectedReminderDate == nil)
                }
            }
            .onDisappear {
                state.selectedReminderDate = nil
            }
        }
    }
}

private struct MainContent: View {
    let bookmark: YabaBookmark
    let reminderDate: Date?
    let folder: YabaCollection?
    let tags: [YabaCollection]
    let mode: DetailMode
    let isLoading: Bool
    let onClickOpenLink: () -> Void
    let onDeleteNotification: () -> Void
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        switch mode {
        case .detail:
            List {
                ImageSection(
                    bookmark: bookmark,
                    onClickOpenLink: onClickOpenLink
                ).redacted(reason: isLoading ? .placeholder : [])
                InfoSection(
                    bookmark: bookmark,
                    reminderDate: reminderDate,
                    onDeleteNotification: onDeleteNotification
                ).redacted(reason: isLoading ? .placeholder : [])
                if let folder {
                    FolderSection(
                        folder: folder,
                        onCollectionNavigationCallback: onCollectionNavigationCallback
                    )
                }
                TagsSection(
                    tags: tags,
                    onCollectionNavigationCallback: onCollectionNavigationCallback
                )
            }
            .scrollContentBackground(.hidden)
        case .reader:
            if let html = bookmark.readableHTML {
                ReaderView(html: html)
            } else {
                ReaderNotAvailableView()
            }
        }
    }
}

private struct ImageSection: View {
    private let openBookmarkTip = BookmarkDetailTip()
    
    let bookmark: YabaBookmark
    let onClickOpenLink: () -> Void
    
    var body: some View {
        Section {
            imageContent
            TipView(openBookmarkTip)
        } header: {
            Label {
                Text("Bookmark Detail Image Header Title")
            } icon: {
                YabaIconView(bundleKey: "image-03")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }.padding(.leading)
        } footer: {
            HStack {
                if let iconData = bookmark.iconDataHolder,
                   let image = UIImage(data: iconData) {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 18, height: 18)
                } else {
                    YabaIconView(bundleKey: "link-02")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
                Text(
                    bookmark.domain.isEmpty
                    ? bookmark.link
                    : bookmark.domain
                ).lineLimit(2)
            }.padding(.leading)
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        .onAppear {
            BookmarkDetailTip.isPresented = true
        }
        .onTapGesture {
            onClickOpenLink()
        }
    }
    
    @ViewBuilder
    private var imageContent: some View {
        if let imageData = bookmark.imageDataHolder,
           let image = UIImage(data: imageData) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(idealHeight: 250, alignment: .center)
        } else {
            ContentUnavailableView {
                Label {
                    Text("Bookmark Detail Image Error Title")
                        .padding(.bottom)
                } icon: {
                    YabaIconView(bundleKey: "image-not-found-01")
                        .scaledToFit()
                        .frame(width: 52, height: 52)
                        .padding(.top)
                }
            } description: {
                Text("Bookmark Detail Image Error Description")
            }
        }
    }
}

private struct InfoSection: View {
    let bookmark: YabaBookmark
    let reminderDate: Date?
    let onDeleteNotification: () -> Void
    
    var body: some View {
        Section {
            HStack(alignment: .top) {
                YabaIconView(bundleKey: "text")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                Text(bookmark.label)
                    .multilineTextAlignment(.leading)
            }
            
            HStack(alignment: .top) {
                YabaIconView(bundleKey: "paragraph")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                if bookmark.bookmarkDescription.isEmpty {
                    Text("Bookmark Detail No Description Provided")
                        .foregroundStyle(.secondary)
                        .italic()
                } else {
                    Text(bookmark.bookmarkDescription)
                        .multilineTextAlignment(.leading)
                }
            }
            
            HStack {
                HStack {
                    YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text("Create Bookmark Type Placeholder")
                }
                Spacer()
                Text(bookmark.bookmarkType.getUITitle())
            }
            
            HStack {
                HStack {
                    YabaIconView(bundleKey: "clock-01")
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text("Bookmark Detail Created At Title")
                }
                Spacer()
                Text(bookmark.createdAt.formatted(date: .abbreviated, time: .shortened))
            }
            
            if bookmark.createdAt != bookmark.editedAt {
                HStack {
                    HStack {
                        YabaIconView(bundleKey: "edit-02")
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundStyle(.tint)
                        Text("Bookmark Detail Edited At Title")
                    }
                    Spacer()
                    Text(bookmark.editedAt.formatted(date: .abbreviated, time: .shortened))
                }
            }
            
            if let reminderDate {
                HStack {
                    HStack {
                        YabaIconView(bundleKey: "notification-01")
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundStyle(.tint)
                        Text("Bookmark Detail Remind Me Title")
                    }
                    Spacer()
                    Text(reminderDate.formatted(date: .abbreviated, time: .shortened))
                }.swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(role: .destructive) {
                        onDeleteNotification()
                    } label: {
                        VStack {
                            YabaIconView(bundleKey: "delete-02")
                            Text("Delete")
                        }
                    }.tint(.red)
                }
            }
        } header: {
            Label {
                Text("Info")
            } icon: {
                YabaIconView(bundleKey: "information-circle")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

private struct FolderSection: View {
    let folder: YabaCollection
    let onCollectionNavigationCallback: (YabaCollection) -> Void

    var body: some View {
        Section {
            CollectionItemView(
                collection: folder,
                isInHome: false,
                isInSelectionMode: false,
                isInBookmarkDetail: true,
                onDeleteCallback: { _ in },
                onEditCallback: { _ in },
                onNavigationCallback: onCollectionNavigationCallback
            )
        } header: {
            Label {
                Text("Folder")
            } icon: {
                YabaIconView(bundleKey: "folder-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

private struct TagsSection: View {
    let tags: [YabaCollection]
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        Section {
            if tags.isEmpty {
                ContentUnavailableView {
                    Label {
                        Text("Bookmark Detail No Tags Added Title")
                    } icon: {
                        YabaIconView(bundleKey: "tags")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("Bookmark Detail No Tags Added Description")
                }
            } else {
                ForEach(tags) { tag in
                    CollectionItemView(
                        collection: tag,
                        isInHome: false,
                        isInSelectionMode: false,
                        isInBookmarkDetail: true,
                        onDeleteCallback: { _ in },
                        onEditCallback: { _ in },
                        onNavigationCallback: onCollectionNavigationCallback
                    )
                }
            }
        } header: {
            Label {
                Text("Tags Title")
            } icon: {
                YabaIconView(bundleKey: "tag-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

#Preview {
    BookmarkDetail(
        bookmark: .empty(),
        onCollectionNavigationCallback: { _ in },
        onDeleteBookmarkCallback: { _ in }
    )
}
