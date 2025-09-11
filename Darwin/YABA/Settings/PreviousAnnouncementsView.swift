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
        ZStack {
            AnimatedGradient(collectionColor: .accentColor)
            List {
                AnnouncementView(
                    titleKey: "Announcement CloudKit Deletion Title",
                    severity: .urgent,
                    isInPreview: true,
                    onClick: {
                        if let url: URL = .init(string: Constants.announcementCloudKitDropLink_3),
                           UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        }
                    },
                    onDismiss: { }
                )
                AnnouncementView(
                    titleKey: "Announcements Legals Update CloudKit Title",
                    severity: .warning,
                    isInPreview: true,
                    onClick: {
                        if let url: URL = .init(string: Constants.announcementLegalsUpdateLink_1),
                           UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        }
                    },
                    onDismiss: { }
                )
                AnnouncementView(
                    titleKey: "Announcements Update Title \("v1.4")",
                    severity: .update,
                    isInPreview: true,
                    onClick: {
                        if let url: URL = .init(string: Constants.updateAnnouncementLink_1_4),
                           UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        }
                    },
                    onDismiss: { }
                )
                AnnouncementView(
                    titleKey: "Announcements CloudKit Support Drop Urgent Title",
                    severity: .urgent,
                    isInPreview: true,
                    onClick: {
                        if let url: URL = .init(string: Constants.announcementCloudKitDropLink_2),
                           UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        }
                    },
                    onDismiss: { }
                )
                AnnouncementView(
                    titleKey: "Announcements Update Title \("v1.3")",
                    severity: .update,
                    isInPreview: true,
                    onClick: {
                        if let url: URL = .init(string: Constants.updateAnnouncementLink_1_3),
                           UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        }
                    },
                    onDismiss: { }
                )
                AnnouncementView(
                    titleKey: "Announcements CloudKit Support Drop Title",
                    severity: .warning,
                    isInPreview: true,
                    onClick: {
                        if let url: URL = .init(string: Constants.announcementCloudKitDropLink_1),
                           UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        }
                    },
                    onDismiss: { }
                )
                AnnouncementView(
                    titleKey: "Announcements Update Title \("v1.2")",
                    severity: .update,
                    isInPreview: true,
                    onClick: {
                        if let url: URL = .init(string: Constants.updateAnnouncementLink_1_2),
                           UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        }
                    },
                    onDismiss: { }
                )
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
            .background(.clear)
        }
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
