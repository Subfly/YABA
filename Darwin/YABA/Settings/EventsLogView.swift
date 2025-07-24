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
    
    @Query(sort: \YabaDataLog.timestamp, order: .reverse)
    private var logs: [YabaDataLog]
    
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .accentColor)
            List {
                ForEach(logs, id: \.id) { log in
                    generateLogView(log)
                }
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
            .background(.clear)
        }
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
    }
    
    @ViewBuilder
    private func generateLogView(_ log: YabaDataLog) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(log.entityType.rawValue) (\(log.entityId))")
                .font(.headline)
                .lineLimit(1)
            HStack {
                Text(log.timestamp.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                Spacer()
                Text(log.actionType.rawValue.capitalized)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            if let fieldChanges = log.fieldChanges, !fieldChanges.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Changes:")
                        .font(.subheadline)
                        .bold()
                    
                    ForEach(fieldChanges, id: \.self) { change in
                        generateChanges(change)
                    }
                }
            }
        }
        .padding(.vertical, 6)
    }
    
    @ViewBuilder
    private func generateChanges(_ change: FieldChange) -> some View {
        HStack(alignment: .top) {
            Text("• \(change.key.rawValue):")
                .bold()
            Spacer()
            Text(change.newValue ?? "")
        }
        .font(.caption)
    }
}
