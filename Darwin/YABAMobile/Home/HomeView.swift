//
//  HomeView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI
import SwiftData

struct HomeView: View {
    @Environment(\.deepLinkManager)
    private var deepLinkManager

    /// Opens global search in the split-view detail column.
    let onOpenSearch: () -> Void
    /// Opens folder bookmark list in the detail column.
    let onSelectFolder: (String) -> Void
    /// Opens tag bookmark list in the detail column.
    let onSelectTag: (String) -> Void
    /// Opens bookmark detail when a supported row is tapped (e.g. link or image recents).
    let onSelectBookmark: (String) -> Void

    @State
    private var homeState: HomeState = .init()

    @State
    private var homeStateMachine = HomeStateMachine()

    var body: some View {
        ZStack {
            AnimatedGradient(color: .accentColor)
            SequentialView(
                onSelectFolder: onSelectFolder,
                onSelectTag: onSelectTag,
                onSelectBookmark: onSelectBookmark
            )
            .scrollContentBackground(.hidden)
            .listStyle(.sidebar)
            
            HomeCreateContentFAB(
                isActive: $homeState.isFABActive,
                onClickAction: { creationType in
                    withAnimation {
                        homeState.isFABActive.toggle()
                        switch creationType {
                        case .tag:
                            homeState.shouldShowCreateTagSheet = true
                        case .folder:
                            homeState.shouldShowCreateFolderSheet = true
                        case .bookmark:
                            homeState.bookmarkTypeSelection = BookmarkTypeSelectionContext()
                        default:
                            break
                        }
                    }
                }
            )
            .transition(.blurReplace)
            .ignoresSafeArea()
        }
        .sheet(isPresented: $homeState.shouldShowCreateFolderSheet) {
            FolderCreationContent()
        }
        .sheet(isPresented: $homeState.shouldShowCreateTagSheet) {
            TagCreationContent()
        }
        .sheet(item: $homeState.bookmarkFlow) { context in
            BookmarkFlowSheet(context: context)
        }
        .bookmarkCreateTwoStepSheets(typeSelection: $homeState.bookmarkTypeSelection)
        // Sync UI disabled (see NetworkSyncManager / SyncView).
        // .sheet(isPresented: $homeState.shouldShowSyncSheet) {
        //     SyncView().interactiveDismissDisabled()
        // }
        .navigationTitle("YABA")
        .task {
            await homeStateMachine.send(.onInit)
        }
        .toolbar {
            // Sync toolbar entry disabled.
            // ToolbarItem(placement: .topBarLeading) {
            //     Button {
            //         homeState.shouldShowSyncSheet = true
            //     } label: {
            //         YabaIconView(bundleKey: "laptop-phone-sync")
            //     }
            // }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    onOpenSearch()
                } label: {
                    YabaIconView(bundleKey: "search-01")
                }
            }
            if #available(iOS 26, *) {
                ToolbarSpacer(.fixed, placement: .topBarTrailing)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    SortingPicker(contentType: .collection)
                    Button {
                        // TODO: SHOW SETTINGS SHEET
                    } label: {
                        Label {
                            Text("Settings Title")
                        } icon: {
                            YabaIconView(bundleKey: "settings-02")
                        }
                    }
                } label: {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                }
            }
        }
        .onChange(of: deepLinkManager.saveRequest) { oldValue, newValue in
            if oldValue == nil, let newRequest = newValue {
                homeState.bookmarkFlow = .deepLink(url: newRequest.link)
                deepLinkManager.onHandleDeeplink()
            }
        }
    }
}

private struct SequentialView: View {
    let onSelectFolder: (String) -> Void
    let onSelectTag: (String) -> Void
    let onSelectBookmark: (String) -> Void

    @AppStorage(Constants.showRecentsKey)
    private var showRecents: Bool = true

    var body: some View {
        List {
            //TODO HomeAnnouncementsView()
            if showRecents && UIDevice.current.userInterfaceIdiom == .phone {
                HomeRecentsView(onSelectBookmark: onSelectBookmark)
            }
            HomeCollectionView(collectionType: .folder, onSelectFolder: onSelectFolder)
            HomeCollectionView(collectionType: .tag, onSelectTag: onSelectTag)
        }.contentMargins(.bottom, 100)
    }
}
