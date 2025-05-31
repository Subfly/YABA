//
//  EventsLogView.swift
//  YABA
//
//  Created by Ali Taha on 30.05.2025.
//

import SwiftUI
import SwiftData

internal struct EventsLogView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Query
    private var logs: [YabaDataLog]
    
    var body: some View {
        List {
            ForEach(logs) { log in
                Text(logs.description)
            }
        }
        .listStyle(.sidebar)
        .navigationTitle("Settings Event Logs Label")
        .toolbar {
            if UIDevice.current.userInterfaceIdiom == .pad {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        dismiss()
                    } label: {
                        Text("Done")
                    }
                }
            } else {
                ToolbarItem(placement: .navigation) {
                    Button {
                        dismiss()
                    } label: {
                        YabaIconView(bundleKey: "arrow-left-01")
                    }
                }
            }
        }
        .onAppear {
            print(logs)
        }
    }
}
