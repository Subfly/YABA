//
//  ChangeBookmarkImageTip.swift
//  YABA
//
//  Created by Ali Taha on 27.09.2025.
//

import Foundation
import SwiftUI
import TipKit

struct ChangeBookmarkImageTip: Tip {
    @Parameter
    static var isPresented: Bool = false
    
    var rules: [Rule] {
        [
            #Rule(Self.$isPresented) {
                $0 == true
            }
        ]
    }
    
    var title: Text {
        Text("Create Bookmark Change Image Tip Title")
    }
    
    var message: Text? {
        Text("Create Bookmark Change Image Tip Message")
    }
    
    var image: Image? {
        Image("ai-image")
            .renderingMode(.template)
            .resizable()
    }
}
