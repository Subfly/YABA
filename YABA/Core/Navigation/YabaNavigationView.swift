//
//  YABANavigationView.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import SwiftUI

struct YabaNavigationView: View {
    @AppStorage(Constants.hasPassedOnboardingKey)
    private var hasPassedOnboarding: Bool = false
    
    @State
    private var appTint: Color = .accentColor
    
    @State
    private var selectedCollection: YabaCollection?
    
    @State
    private var selectedBookmark: Bookmark?
    
    var body: some View {
        NavigationSplitView {
            HomeView(
                selectedCollection: $selectedCollection,
                selectedAppTint: $appTint
            )
        } content: {
            CollectionDetail(
                collection: $selectedCollection,
                selectedBookmark: $selectedBookmark
            )
        } detail: {
            Text("Lele Detial")
        }
        .tint(appTint)
        .onChange(of: selectedCollection) { _, newValue in
            if let color = newValue?.color.getUIColor() {
                withAnimation {
                    appTint = color
                }
            }
        }
    }
}

#Preview {
    YabaNavigationView()
}
