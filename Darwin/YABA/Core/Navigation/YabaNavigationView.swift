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
    var body: some View {
        NavigationSplitView {
            HomeView()
        } detail: {
            EmptyView()
        }
    }
}

#Preview {
    YabaNavigationView()
}
