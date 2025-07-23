//
//  PreviousAnnouncementsView.swift
//  YABA
//
//  Created by Ali Taha on 23.07.2025.
//

import SwiftUI

internal struct PreviousAnnouncementsView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    var body: some View {
        List {
            AnnouncementView(
                titleKey: "Announcements CloudKit Support Drop Title",
                severity: .warning,
                isInPreview: true,
                onClick: {
                    if let url: URL = .init(string: Constants.announcementCloudKitDropLink),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: { }
            )
            AnnouncementView(
                titleKey: "Announcements Update Title",
                severity: .update,
                isInPreview: true,
                onClick: {
                    if let url: URL = .init(string: Constants.updateAnnouncementLink),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: { }
            )
        }
        .listStyle(.sidebar)
        .navigationTitle("Settings Announcements Title")
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    dismiss()
                } label: {
                    YabaIconView(bundleKey: "arrow-left-01")
                }
            }
        }
    }
}

#Preview {
    PreviousAnnouncementsView()
}
