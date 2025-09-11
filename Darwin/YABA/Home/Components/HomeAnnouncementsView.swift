//
//  HomeAnnouncementsView.swift
//  YABA
//
//  Created by Ali Taha on 22.07.2025.
//

import SwiftUI

struct HomeAnnouncementsView: View {
    @AppStorage(Constants.announcementsYaba1_4UpdateKey)
    private var showUpdateAnnouncement: Bool = true
    
    @AppStorage(Constants.announcementsLegalsUpdateKey)
    private var showLegalsAnnouncement: Bool = true
    
    @AppStorage(Constants.announcementsCloudKitDatabaseWipeKey)
    private var showCloudKitAnnouncement: Bool = true
    
    var body: some View {
        if showUpdateAnnouncement || showLegalsAnnouncement || showCloudKitAnnouncement {
            Section {
                content.transition(.blurReplace)
            } header: {
                Label {
                    Text("Home Announcements Title")
                } icon: {
                    YabaIconView(bundleKey: "megaphone-03")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
            }
        }
    }
    
    @ViewBuilder
    private var content: some View {
        if showUpdateAnnouncement {
            AnnouncementView(
                titleKey: "Announcements Update Title \("v1.4")",
                severity: .update,
                isInPreview: false,
                onClick: {
                    if let url: URL = .init(string: Constants.updateAnnouncementLink_1_4),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: {
                    showUpdateAnnouncement = false
                }
            ).animation(.smooth, value: showUpdateAnnouncement)
        }
        if showLegalsAnnouncement {
            AnnouncementView(
                titleKey: "Announcements Legals Update CloudKit Title",
                severity: .warning,
                isInPreview: false,
                onClick: {
                    if let url: URL = .init(string: Constants.announcementLegalsUpdateLink_1),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: {
                    showLegalsAnnouncement = false
                }
            ).animation(.smooth, value: showLegalsAnnouncement)
        }
        if showCloudKitAnnouncement {
            AnnouncementView(
                titleKey: "Announcement CloudKit Deletion Title",
                severity: .urgent,
                isInPreview: false,
                onClick: {
                    if let url: URL = .init(string: Constants.announcementCloudKitDropLink_3),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: {
                    showCloudKitAnnouncement = false
                }
            ).animation(.smooth, value: showCloudKitAnnouncement)
        }
    }
}

#Preview {
    HomeAnnouncementsView()
}
