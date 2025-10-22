//
//  InvisibleSeparatorItem.swift
//  YABA
//
//  Used to create a drop target for custom
//  sorting of folders and tags.
//
//  Created by Ali Taha on 10.10.2025.
//

import SwiftUI

struct SeparatorItemView: View {
    @Environment(\.moveManager)
    private var moveManager
    
    @State
    private var isTargeted: Bool = false
    
    var body: some View {
        color
            .frame(height: 3)
            .contentShape(RoundedRectangle(cornerRadius: 8))
            .padding(.horizontal)
            .padding(.horizontal)
            .onDrop(
                of: [.yabaCollection],
                isTargeted: $isTargeted
            ) { providers in
                return false
            }
            .sensoryFeedback(.impact(weight: .heavy, intensity: 1), trigger: isTargeted)
    }
    
    @ViewBuilder
    private var color: some View {
        isTargeted ? Color.blue.opacity(0.6) : Color.clear
    }
}

#Preview {
    SeparatorItemView()
}
