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

    @State
    private var homeState: HomeState = .init()

    @State
    private var homeStateMachine = HomeStateMachine()

    var body: some View {
        ZStack {
            #if !targetEnvironment(macCatalyst)
            AnimatedGradient(color: .accentColor)
            #endif
            SequentialView(
                onSelectFolder: onSelectFolder,
                onSelectTag: onSelectTag
            )
                .scrollContentBackground(.hidden)
                #if targetEnvironment(macCatalyst)
                .listStyle(.sidebar)
                #endif
            
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
        // #if !targetEnvironment(macCatalyst)
        // .fullScreenCover(isPresented: $homeState.shouldShowSyncSheet) {
        //     SyncView().interactiveDismissDisabled()
        // }
        // #else
        // .sheet(isPresented: $homeState.shouldShowSyncSheet) {
        //     SyncView().interactiveDismissDisabled()
        // }
        // #endif
        .navigationTitle("YABA")
        .task {
            await homeStateMachine.send(.onInit)
        }
        .toolbar {
            // Sync toolbar entry disabled.
            // ToolbarItem(placement: .topBarLeading) {
            //     #if targetEnvironment(macCatalyst)
            //     MacOSHoverableToolbarIcon(
            //         bundleKey: "laptop-phone-sync",
            //         tooltipKey: "Synchronize Label",
            //         onPressed: {
            //             homeState.shouldShowSyncSheet = true
            //         }
            //     )
            //     #else
            //     Button {
            //         homeState.shouldShowSyncSheet = true
            //     } label: {
            //         YabaIconView(bundleKey: "laptop-phone-sync")
            //     }
            //     #endif
            // }
            ToolbarItem(placement: .topBarTrailing) {
                #if targetEnvironment(macCatalyst)
                MacOSHoverableToolbarIcon(
                    bundleKey: "search-01",
                    tooltipKey: "Search Title",
                    onPressed: onOpenSearch
                )
                #else
                Button {
                    onOpenSearch()
                } label: {
                    YabaIconView(bundleKey: "search-01")
                }
                #endif
            }
            if #available(iOS 26, *) {
                ToolbarSpacer(.fixed, placement: .topBarTrailing)
            }
            ToolbarItem(placement: .topBarTrailing) {
                #if targetEnvironment(macCatalyst)
                Menu {
                    SortingPicker(contentType: .collection)
                    Button {
                        // TODO: SHOW SETTINGS (Catalyst)
                    } label: {
                        Label {
                            Text("Settings Title")
                        } icon: {
                            YabaIconView(bundleKey: "settings-02")
                        }
                    }
                } label: {
                    MacOSHoverableToolbarIcon(
                        bundleKey:  "more-horizontal-circle-02",
                        tooltipKey: "Show More Label",
                        onPressed: {}
                    )
                }
                #else
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
                #endif
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

    @AppStorage(Constants.showRecentsKey)
    private var showRecents: Bool = true

    var body: some View {
        List {
            //TODO HomeAnnouncementsView()
            if showRecents && UIDevice.current.userInterfaceIdiom == .phone {
                HomeRecentsView()
            }
            HomeCollectionView(collectionType: .folder, onSelectFolder: onSelectFolder)
            HomeCollectionView(collectionType: .tag, onSelectTag: onSelectTag)
        }
    }
}
