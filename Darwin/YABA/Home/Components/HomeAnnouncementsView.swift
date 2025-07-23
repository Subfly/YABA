//
//  HomeAnnouncementsView.swift
//  YABA
//
//  Created by Ali Taha on 22.07.2025.
//

import SwiftUI

struct HomeAnnouncementsView: View {
    @AppStorage(Constants.announcementsYaba1_2UpdateKey)
    private var showUpdateAnnouncement: Bool = true
    
    @AppStorage(Constants.announcementsCloudKitDropKey)
    private var showCloudKitAnnouncement: Bool = true
    
    var body: some View {
        if showUpdateAnnouncement || showCloudKitAnnouncement {
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
                titleKey: "Announcements Update Title",
                severity: .update,
                isInPreview: false,
                onClick: {
                    if let url: URL = .init(string: Constants.updateAnnouncementLink),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: {
                    showUpdateAnnouncement = false
                }
            ).animation(.smooth, value: showUpdateAnnouncement)
        }
        if showCloudKitAnnouncement {
            AnnouncementView(
                titleKey: "Announcements CloudKit Support Drop Title",
                severity: .warning,
                isInPreview: false,
                onClick: {
                    if let url: URL = .init(string: Constants.announcementCloudKitDropLink),
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
