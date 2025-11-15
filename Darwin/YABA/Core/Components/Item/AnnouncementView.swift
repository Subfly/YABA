//
//  AnnouncementView.swift
//  YABA
//
//  Created by Ali Taha on 23.07.2025.
//

import SwiftUI

struct AnnouncementView: View {
    @State
    private var isHovered: Bool = false
    
    @State
    private var showDeleteDialog: Bool = false
    
    let titleKey: LocalizedStringKey
    let severity: AnnouncementSeverity
    let isInPreview: Bool
    let onClick: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        viewSwitcher
            .onHover { hovered in
                isHovered = hovered
            }
            .alert(
                "Announcements Delete Title",
                isPresented: $showDeleteDialog,
            ) {
                Button(role: .cancel) {
                    showDeleteDialog = false
                } label: {
                    Text("Cancel")
                }
                Button(role: .destructive) {
                    withAnimation {
                        onDismiss()
                        showDeleteDialog = false
                    }
                } label: {
                    Text("Delete")
                }
            } message: {
                Text("Announcements Delete Message")
            }
            .onTapGesture {
                onClick()
            }
    }
    
    @ViewBuilder
    private var viewSwitcher: some View {
        if !isInPreview {
            baseView
                .padding()
                .background {
                    #if targetEnvironment(macCatalyst)
                    if isHovered {
                        RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.1))
                    } else {
                        RoundedRectangle(cornerRadius: 16).fill(Color.clear)
                    }
                    #else
                    if UIDevice.current.userInterfaceIdiom == .phone {
                        RoundedRectangle(cornerRadius: 24).fill(Color.gray.opacity(0.05))
                    } else if isHovered {
                        RoundedRectangle(cornerRadius: 24).fill(Color.gray.opacity(0.1))
                    } else {
                        RoundedRectangle(cornerRadius: 24).fill(Color.clear)
                    }
                    #endif
                }
                .padding(.horizontal)
                .contextMenu {
                    if !isInPreview {
                        Button {
                            showDeleteDialog = true
                        } label: {
                            VStack {
                                YabaIconView(bundleKey: "delete-02")
                                Text("Delete")
                            }
                        }.tint(.red)
                    }
                }
        } else {
            baseView
            #if targetEnvironment(macCatalyst)
                .listRowBackground(
                    isHovered
                    ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
                    : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
                )
            #endif
        }
    }
    
    @ViewBuilder
    private var baseView: some View {
        HStack {
            HStack {
                YabaIconView(bundleKey: severity.getUIIcon())
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(severity.getUIColor())
                Text(titleKey)
            }
            Spacer()
            HStack {
                if isHovered && !isInPreview {
                    YabaIconView(bundleKey: "delete-02")
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.red)
                        .onTapGesture {
                            showDeleteDialog = true
                        }
                }
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tertiary)
            }
        }
        .contentShape(Rectangle())
    }
}
