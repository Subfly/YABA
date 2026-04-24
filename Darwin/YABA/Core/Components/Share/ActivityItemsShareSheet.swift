//
//  ActivityItemsShareSheet.swift
//  YABA
//
//  UIActivityViewController wrapper for arbitrary share items (files, images, etc.).
//

import SwiftUI
import UIKit

struct ActivityItemsShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let vc = UIActivityViewController(activityItems: items, applicationActivities: nil)
        vc.modalPresentationStyle = .formSheet
        return vc
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
