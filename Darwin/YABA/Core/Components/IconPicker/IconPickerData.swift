//
//  IconPickerData.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//
//  Hierarchical icon selection: Categories -> Subcategories -> Icons
//
//  Usage:
//  1. categories and subcategories are loaded from icon_categories_header.json on init
//  2. Call loadIcons(for:subcategory:) to populate currentIcons
//  3. currentIcons contains the icons to display in UI
//

import Foundation
import SwiftUI

@MainActor
@Observable
internal class IconPickerData {
    @ObservationIgnored
    private var iconHeader: IconHeader?
    
    @ObservationIgnored
    private var loadedSubcategoryData: [String: [YabaIcon]] = [:]
    
    // Public properties for UI
    var categories: [IconCategory] = []
    var currentIcons: [YabaIcon] = []
    
    init() {
        loadHeaderData()
    }
    
    /// Load icons for a specific subcategory
    func loadIcons(for subcategory: IconSubcategory) {
        // Check if icons for this subcategory are already loaded
        if let cachedIcons = loadedSubcategoryData[subcategory.id] {
            currentIcons = cachedIcons
            return
        }
        
        // Load icons from the subcategory's JSON file
        loadSubcategoryData(subcategory)
        currentIcons = loadedSubcategoryData[subcategory.id] ?? []
    }
    
    /// Clear current selection and reset to categories view
    func resetSelection() {
        currentIcons = []
    }
    
    private func loadHeaderData() {
        guard let url = Bundle.main.url(forResource: "icon_categories_header", withExtension: "json") else {
            print("Failed to find icon_categories_header.json")
            return
        }
        
        guard let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode(IconHeader.self, from: data) else {
            print("Failed to decode icon_categories_header.json")
            return
        }
        
        iconHeader = decoded
        categories = decoded.categories
    }
    
    private func loadSubcategoryData(_ subcategory: IconSubcategory) {
        let filename = subcategory.filename.replacingOccurrences(of: ".json", with: "")
        
        guard let url = Bundle.main.url(forResource: filename, withExtension: "json") else {
            print("Failed to find \(filename).json")
            return
        }
        
        guard let data = try? Data(contentsOf: url) else {
            print("Failed to load data from \(filename).json")
            return
        }
        
        // Try to decode as a subcategory file with metadata and icons array
        if let decoded = try? JSONDecoder().decode(SubcategoryIconData.self, from: data) {
            loadedSubcategoryData[subcategory.id] = decoded.icons.sorted { $0.name < $1.name }
        } else {
            print("Failed to decode \(filename).json")
        }
    }
}

// MARK: - Supporting Models

private struct SubcategoryIconData: Codable {
    let metadata: SubcategoryMetadata
    let icons: [YabaIcon]
}

private struct SubcategoryMetadata: Codable {
    let id: String
    let name: String
    let description: String
    let mainCategory: String
    let iconCount: Int
    let version: String
    
    enum CodingKeys: String, CodingKey {
        case id, name, description, version
        case mainCategory = "main_category"
        case iconCount = "icon_count"
    }
}
