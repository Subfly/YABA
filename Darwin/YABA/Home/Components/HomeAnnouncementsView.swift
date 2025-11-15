//
//  HomeAnnouncementsView.swift
//  YABA
//
//  Created by Ali Taha on 22.07.2025.
//

import SwiftUI

struct HomeAnnouncementsView: View {
    @AppStorage(Constants.announcementsYaba1_5UpdateKey)
    private var showUpdateAnnouncement: Bool = true
    
    @AppStorage(Constants.announcementsLegalsUpdate_2Key)
    private var showLegalsAnnouncement: Bool = true
    
    var body: some View {
        if showUpdateAnnouncement || showLegalsAnnouncement {
            HStack {
                Label {
                    Text("Home Announcements Title")
                        .font(.headline)
                        .fontWeight(.semibold)
                } icon: {
                    YabaIconView(bundleKey: "megaphone-03")
                        .scaledToFit()
                        .frame(width: 22, height: 22)
                }
                .foregroundStyle(.secondary)
                .font(.headline)
                Spacer()
            }
            .padding(.horizontal)
            .padding(.bottom)
            content.transition(.blurReplace)
        }
    }
    
    @ViewBuilder
    private var content: some View {
        if showUpdateAnnouncement {
            AnnouncementView(
                titleKey: "Announcements Update Title \("v1.5")",
                severity: .update,
                isInPreview: false,
                onClick: {
                    if let url: URL = .init(string: Constants.updateAnnouncementLink_1_5),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: {
                    showUpdateAnnouncement = false
                }
            ).animation(.smooth, value: showUpdateAnnouncement)
            Spacer().frame(height: 6)
        }
        if showLegalsAnnouncement {
            AnnouncementView(
                titleKey: "Announcements Legals Update MIT Title",
                severity: .warning,
                isInPreview: false,
                onClick: {
                    if let url: URL = .init(string: Constants.announcementLegalsUpdateLink_2),
                       UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    }
                },
                onDismiss: {
                    showLegalsAnnouncement = false
                }
            ).animation(.smooth, value: showLegalsAnnouncement)
            Spacer().frame(height: 6)
        }
    }
}

#Preview {
    HomeAnnouncementsView()
}
