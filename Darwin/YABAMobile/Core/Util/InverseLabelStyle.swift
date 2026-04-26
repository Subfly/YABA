//
//  InverseLabelStyle.swift
//  YABA
//
//  Created by Ali Taha on 3.05.2025.
//

import SwiftUI

struct InverseLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack {
            configuration.title
            configuration.icon
        }
    }
}
