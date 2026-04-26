//
//  BookmarkCreationPreviewListRowBackground.swift
//  YABA
//

import SwiftUI
import UIKit

extension View {
    /// Grid previews draw their own material; this fades out the grouped list row fill so it does not stack behind the card.
    func bookmarkCreationPreviewListRowBackground(appearance: PreviewContentAppearance) -> some View {
        listRowBackground(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color(uiColor: .secondarySystemGroupedBackground))
                .opacity(appearance == .grid ? 0 : 1)
                .animation(.smooth, value: appearance)
        )
    }
}
