//
//  ShareSheet.swift
//  YABA
//
//  Created by Ali Taha on 4.06.2025.
//


import SwiftUI

struct ShareSheet: UIViewControllerRepresentable {
    let bookmarkLink: URL
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let activityItems: [Any] = [self.bookmarkLink]
        let activityViewController = UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
        activityViewController.modalPresentationStyle = .formSheet
        #if !os(visionOS)
        let detents: [UISheetPresentationController.Detent] = [.medium()]
        activityViewController.sheetPresentationController?.detents = detents
        #endif
        return activityViewController
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {
        // No updates needed
    }
}

#Preview {
    if let url = URL(string: "") {
        ShareSheet(bookmarkLink: url)
    }
}
