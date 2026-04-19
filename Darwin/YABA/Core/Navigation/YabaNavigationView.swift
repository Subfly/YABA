//
//  YABANavigationView.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import SwiftUI

/// Root shell for the Darwin UI rebuild. Previous navigation (home, detail, onboarding, etc.) is archived
/// under `YABA/Home`, `YABA/Detail`, `YABA/Onboarding`, and related folders.
struct YabaNavigationView: View {
    @Environment(\.horizontalSizeClass)
    private var horizontalSizeClass

    @State
    private var detailRouter = DetailColumnRouter()

    var body: some View {
        ZStack(alignment: .bottom) {
            if shouldUseCompactNavigation {
                NavigationStack(path: Bindable(detailRouter).navigationPath) {
                    homeContent
                        .navigationDestination(for: DetailDestination.self) { destination in
                            destinationView(for: destination)
                        }
                }
            } else {
                NavigationSplitView {
                    homeContent
                } detail: {
                    NavigationStack(path: Bindable(detailRouter).navigationPath) {
                        DetailColumnPlaceholderView()
                            .navigationDestination(for: DetailDestination.self) { destination in
                                destinationView(for: destination)
                            }
                    }
                }
            }
            CoreToastOverlayView()
        }
    }

    private var shouldUseCompactNavigation: Bool {
        #if targetEnvironment(macCatalyst)
        false
        #else
        UIDevice.current.userInterfaceIdiom == .phone || horizontalSizeClass == .compact
        #endif
    }

    private var homeContent: some View {
        HomeView(
            onOpenSearch: { detailRouter.openSearch() },
            onSelectFolder: { detailRouter.openFolder(id: $0) },
            onSelectTag: { detailRouter.openTag(id: $0) }
        )
    }

    @ViewBuilder
    private func destinationView(for destination: DetailDestination) -> some View {
        switch destination {
        case .search:
            SearchView()
        case let .folder(id):
            FolderDetailView(folderId: id)
        case let .tag(id):
            TagDetailView(tagId: id)
        }
    }
}
