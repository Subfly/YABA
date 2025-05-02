//
//  IconPickerData.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import Foundation
import SwiftUI

@MainActor
@Observable
class IconPickerData {
    @ObservationIgnored
    private var readIcons: [YabaIcon] = []
    
    var icons: [YabaIcon] = []
    
    init() {
        readJson()
    }
    
    func onQueryChange(_ newQuery: String) {
        icons = if newQuery.isEmpty {
            readIcons.sorted(by: { icon1, icon2 in
                icon1.name < icon2.name
            })
        } else {
            readIcons.filter {
                $0.name.lowercased().localizedStandardContains(newQuery.lowercased())
                || $0.tags.lowercased().localizedStandardContains(newQuery.lowercased())
            }.sorted(by: { icon1, icon2 in
                icon1.name < icon2.name
            })
        }
    }
    
    private func readJson() {
        guard let url = Bundle.main.url(forResource: "yaba_icons", withExtension: "json") else {
            return
        }
        
        guard let data = try? Data(contentsOf: url),
           let decoded = try? JSONDecoder().decode(PreloadYabaIconHolder.self, from: data) else {
            return
        }
        
        readIcons = decoded.icons
        onQueryChange("")
    }
}
