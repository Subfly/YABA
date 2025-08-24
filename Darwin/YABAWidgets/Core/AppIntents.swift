//
//  AppIntent.swift
//  YABAWidgets
//
//  Created by Ali Taha on 22.08.2025.
//

import WidgetKit
import AppIntents

internal struct BookmarkListAppIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { "Bookmark List Configuration Title" }
    static var description: IntentDescription { "Bookmark List Configuration Description" }
}
