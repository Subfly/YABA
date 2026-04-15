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
    
    @State
    private var homeState: HomeState = .init()
    
    var body: some View {
        ZStack {
            #if !targetEnvironment(macCatalyst)
            AnimatedGradient(color: .accentColor)
            #endif
            SequentialView()
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
                            homeState.shouldShowCreateBookmarkSheet = true
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
        .sheet(isPresented: $homeState.shouldShowCreateBookmarkSheet) {
            // TODO: SHOW BOOKMARK CREATION CONTENT
        }
        .sheet(item: $homeState.saveBookmarkRequest) { request in
            // TODO: SHOW BOOKMARK CREATION CONTENT
        }
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
                    onPressed: {
                        // TODO: SHOW SEARCH VIEW IN DETAIL
                    }
                )
                #else
                Button {
                    // TODO: SHOW SEARCH VIEW IN DETAIL
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
                        onNavigationCallbackForSettings()
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
            if oldValue == nil {
                if let newRequest = newValue {
                    homeState.saveBookmarkRequest = newRequest
                    deepLinkManager.onHandleDeeplink()
                }
            }
        }
    }
}

private struct SequentialView: View {
    @AppStorage(Constants.showRecentsKey)
    private var showRecents: Bool = true
        
    var body: some View {
        List {
            //TODO HomeAnnouncementsView()
            if showRecents && UIDevice.current.userInterfaceIdiom == .phone {
                //TODO HomeRecentsView(onNavigationCallback: onNavigationCallbackForBookmark)
            }
            HomeCollectionView(collectionType: .folder)
            HomeCollectionView(collectionType: .tag)
        }
    }
}
