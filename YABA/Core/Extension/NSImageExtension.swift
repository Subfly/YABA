//
//  NSImageExtension.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

#if os(macOS)
import Cocoa

typealias UIImage = NSImage

extension NSImage {
    // Taken from https://levelup.gitconnected.com/swift-macos-nsimage-to-png-data-3f7d1543217b
    var data: Data? {
        guard let cgImage = self.cgImage(forProposedRect: nil, context: nil, hints: nil) else { return nil }
        let rep = NSBitmapImageRep(cgImage: cgImage)
        rep.size = self.size
        return rep.representation(using: .png, properties: [:])
    }
}
#endif
