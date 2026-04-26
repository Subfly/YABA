//
//  YabaIconView.swift
//  YABA
//
//  Created by Ali Taha on 2.05.2025.
//

import SwiftUI

struct YabaIconView: View {
    let bundleKey: String
    
    var body: some View {
        Image(bundleKey)
            .renderingMode(.template)
            .resizable()
    }
}

#Preview {
    YabaIconView(bundleKey: "qr-code-01")
}
