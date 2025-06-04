//
//  BookmarkDetailTip.swift
//  YABA
//
//  Created by Ali Taha on 4.06.2025.
//

import Foundation
import SwiftUI
import TipKit

struct BookmarkDetailTip: Tip {
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
        Text("Bookmark Detail Open Link Tip Title")
    }
    
    var message: Text? {
        Text("Bookmark Detail Open Link Tip Message")
    }
    
    var image: Image? {
        Image("book-open-02")
            .renderingMode(.template)
            .resizable()
    }
}
