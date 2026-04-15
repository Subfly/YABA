//
//  YABAWidgetsBundle.swift
//  YABAWidgets
//
//  Temporary placeholder widget while widget implementations are archived.
//

import WidgetKit
import SwiftUI

private struct PlaceholderEntry: TimelineEntry {
    let date: Date
}

private struct PlaceholderProvider: TimelineProvider {
    func placeholder(in context: Context) -> PlaceholderEntry {
        PlaceholderEntry(date: Date())
    }

    func getSnapshot(in context: Context, completion: @escaping (PlaceholderEntry) -> Void) {
        completion(PlaceholderEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PlaceholderEntry>) -> Void) {
        let entry = PlaceholderEntry(date: Date())
        completion(Timeline(entries: [entry], policy: .never))
    }
}

private struct YABAPlaceholderWidget: Widget {
    let kind = "dev.yaba.placeholder"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PlaceholderProvider()) { _ in
            Text("YABA")
        }
        .configurationDisplayName("YABA")
        .description("Placeholder")
    }
}

@main
struct YABAWidgetsBundle: WidgetBundle {
    @WidgetBundleBuilder
    var body: some Widget {
        YABAPlaceholderWidget()
    }
}


// --- ARCHIVED (original file, line-commented; not compiled) ---
// //
// //  YABAWidgetsBundle.swift
// //  YABAWidgets
// //
// //  Created by Ali Taha on 22.08.2025.
// //
// 
// import WidgetKit
// import SwiftUI
// 
// @main
// struct YABAWidgetsBundle: WidgetBundle {
//     @WidgetBundleBuilder
//     var body: some Widget {
//         CategoryWidget()
//         BookmarkWidget()
//         YABALauncherControlCenterWidget()
//         // What a bs that apple does not let use of URL types in here
//         // TODO: ENABLE ONCE SHORTCUTS ARE THERE
//         //QuickmarkControlCenterWidget()
//     }
// }
