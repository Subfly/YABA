//
//  YABAWidgetsBundle.swift
//  YABAWidgets
//
//  Created by Ali Taha on 22.08.2025.
//

import WidgetKit
import SwiftUI

@main
struct YABAWidgetsBundle: WidgetBundle {
    @WidgetBundleBuilder
    var body: some Widget {
        BookmarkWidget()
    }
}
