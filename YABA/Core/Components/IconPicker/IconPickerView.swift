//
//  IconPickerView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI

struct IconPickerView: View {
    @State
    private var searchQuery: String = ""
    
    @State
    private var data: IconPickerData = .init()
    
    private let columns = [
        GridItem(),
        GridItem(),
        GridItem(),
        GridItem()
    ]
    
    let onSelectIcon: (String) -> Void
    let onCancel: () -> Void
    
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
            ScrollView {
                LazyVGrid(columns: columns, spacing: 32) {
                    ForEach(data.iconKeys, id: \.self) { iconKey in
                        let subIcons = data.getIconValues(for: iconKey)
                        PickableIcon(
                            key: iconKey,
                            subIcons: subIcons,
                            onSelectIcon: onSelectIcon
                        )
                    }
                }
                .safeAreaPadding(.top)
            }
            .searchable(text: $searchQuery, prompt: "Search Icons")
            .onChange(of: searchQuery) { _, newQuery in
                withAnimation {
                    data.onQueryChange(newQuery)
                }
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        onCancel()
                    }
                }
            }
            .navigationTitle("Pick Icon")
        }
    }
    
    @ViewBuilder
    private var macOSView: some View {
        VStack {
            VStack {
                HStack {
                    Text("Pick Icon")
                        .font(.title3)
                        .fontWeight(.semibold)
                    Spacer()
                    TextField(
                        "",
                        text: $searchQuery,
                        prompt: Text("Search Icons")
                    ).frame(width: 150)
                }.padding(.horizontal)
                Spacer()
            }
            .padding(.horizontal)
            .padding(.top)
            ScrollView {
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(data.iconKeys, id: \.self) { iconKey in
                        let subIcons = data.getIconValues(for: iconKey)
                        PickableIcon(
                            key: iconKey,
                            subIcons: subIcons,
                            onSelectIcon: onSelectIcon
                        )
                    }
                }
                .safeAreaPadding(.top)
                .padding(.horizontal)
            }
            .frame(width: 300, height: 300)
            .onChange(of: searchQuery) { _, newQuery in
                withAnimation {
                    data.onQueryChange(newQuery)
                }
            }
            Divider()
            HStack {
                Spacer()
                Button("Cancel", role: .cancel) {
                    onCancel()
                }
            }
            .padding(.horizontal)
            .padding(.bottom)
        }
    }
}

private struct PickableIcon: View {
    let key: String
    let subIcons: [String]
    let onSelectIcon: (String) -> Void
    
    @State
    private var showVariantSelectionSheet: Bool = false
    
    private let columns = [
        GridItem(),
        GridItem(),
        GridItem(),
        GridItem()
    ]
    
    var body: some View {
        #if os(iOS)
        iOSView
        #elseif os(macOS)
        MacOSPickableIcon(
            showVariantSelectionSheet: $showVariantSelectionSheet,
            key: key,
            subIcons: subIcons,
            onSelectIcon: onSelectIcon
        )
        #endif
    }
    
    @ViewBuilder
    private var iOSView: some View {
        Image(systemName: subIcons.first ?? key)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 44, height: 44)
            .onTapGesture {
                if subIcons.count == 1 {
                    onSelectIcon(key)
                } else {
                    showVariantSelectionSheet = true
                }
            }
            .sheet(isPresented: $showVariantSelectionSheet) {
                NavigationView {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 32) {
                            ForEach(subIcons, id: \.self) { iconKey in
                                Image(systemName: iconKey)
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 44, height: 44)
                                    .onTapGesture {
                                        onSelectIcon(iconKey)
                                    }
                            }
                        }
                        .safeAreaPadding(.top)
                    }
                    .navigationTitle("Pick Icon Variant")
                    #if os(iOS)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel", role: .cancel) {
                                showVariantSelectionSheet = false
                            }
                        }
                    }
                    #endif
                }.presentationDetents([.fraction(subIcons.count > 4 ? 0.5 : 0.25)])
            }
    }
}

private struct MacOSPickableIcon: View {
    @State
    private var isHovered: Bool = false
    
    @Binding
    var showVariantSelectionSheet: Bool
    let key: String
    let subIcons: [String]
    let onSelectIcon: (String) -> Void
    
    var body: some View {
        Image(systemName: subIcons.first ?? key)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 32, height: 32)
            .background {
                if isHovered {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(.tertiary.opacity(0.5))
                        .frame(width: 42, height: 42)
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
                if subIcons.count == 1 {
                    onSelectIcon(key)
                } else {
                    showVariantSelectionSheet = true
                }
            }
            .popover(isPresented: $showVariantSelectionSheet) {
                VStack {
                    Text("Pick Icon Variant")
                        .font(.title2)
                        .fontWeight(.bold)
                    HStack {
                        ForEach(subIcons, id: \.self) { iconKey in
                            Image(systemName: iconKey)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 44, height: 44)
                                .onTapGesture {
                                    onSelectIcon(iconKey)
                                }
                        }
                    }
                }.padding()
            }
    }
}

#Preview {
    IconPickerView(onSelectIcon: { _ in }, onCancel: {})
}
