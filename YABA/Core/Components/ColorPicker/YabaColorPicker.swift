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
        #if os(iOS)
        iOSView
        #elseif os(macOS)
        macOSView
        #endif
    }
    
    @ViewBuilder
    private var iOSView: some View {
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
            #if os(iOS)
            .pickerStyle(.wheel)
            .navigationBarTitleDisplayMode(.inline)
            #endif
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
        .presentationDragIndicator(.visible)
    }
    
    @ViewBuilder
    private var macOSView: some View {
        VStack {
            Text("Select Color Title")
                .font(.title3)
                .fontWeight(.semibold)
                .padding(.top, 8)
            ScrollView {
                ForEach(YabaColor.allCases, id: \.self) { color in
                    MacOSItemView(
                        color: color,
                        selection: $selection,
                        onDismiss: onDismiss
                    )
                }.safeAreaPadding([.bottom])
            }.frame(width: 150, height: 100)
        }
    }
}

private struct MacOSItemView: View {
    @State
    private var isHovered: Bool = false
    
    var color: YabaColor
    @Binding
    var selection: YabaColor
    var onDismiss: () -> Void
    
    var body: some View {
        HStack {
            HStack {
                Circle()
                    .fill(color.getUIColor())
                    .frame(width: 8, height: 8)
                Text(color.getUIText())
            }
            Spacer()
            if color == selection {
                Image(systemName: "checkmark")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 8, height: 8)
                    .padding(.trailing, 4)
            }
        }
        .padding(.horizontal)
        .background {
            if isHovered {
                RoundedRectangle(cornerRadius: 12)
                    .fill(.tertiary.opacity(0.5))
                    .padding(.horizontal, 12)
                    .frame(height: 18)
            } else {
                Rectangle().fill(.clear)
            }
        }
        .onHover { hovered in
            withAnimation {
                isHovered = hovered
            }
        }
        .onTapGesture {
            withAnimation {
                selection = color
                onDismiss()
            }
        }
    }
}

#Preview {
    YabaColorPicker(
        selection: .constant(.blue),
        onDismiss: {}
    )
}
