//
//  ColorPicker.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import SwiftUI

struct YabaColorPicker: View {
    @Binding
    var selection: YabaColor
    var onDismiss: () -> Void
    
    var body: some View {
        NavigationView {
            Picker(
                selection: $selection,
                content: {
                    ForEach(YabaColor.allCases, id: \.self) { color in
                        HStack {
                            Circle()
                                .foregroundStyle(color.getUIColor())
                                .frame(width: 12, height: 12, alignment: .center)
                            Text(color.getUIText())
                        }
                    }
                },
                label: {
                    Label {
                        Text("Select Color Title")
                    } icon: {
                        Image(systemName: "swatchpalette")
                    }
                }
            )
            .pickerStyle(.wheel)
            .navigationBarTitleDisplayMode(.inline)
            .navigationTitle("Select Color Title")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        onDismiss()
                    } label: {
                        Text("Done")
                    }
                }
            }
            .onDisappear {
                onDismiss()
            }
        }
        .presentationDetents([.fraction(0.3)])
        #if !targetEnvironment(macCatalyst)
        .presentationDragIndicator(.visible)
        #endif
    }
}

#Preview {
    YabaColorPicker(
        selection: .constant(.blue),
        onDismiss: {}
    )
}
