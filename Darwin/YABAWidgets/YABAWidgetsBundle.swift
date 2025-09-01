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
        CategoryWidget()
        BookmarkWidget()
        YABALauncherControlCenterWidget()
        // What a bs that apple does not let use of URL types in here
        // TODO: ENABLE ONCE SHORTCUTS ARE THERE
        //QuickmarkControlCenterWidget()
    }
}
