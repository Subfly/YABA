//
//  YabaImageCompression.swift
//  YABACore
//
//  Parity with Compose `ImageCompression`: maps stored 0...50 to effective quality 100...50.
//

import Foundation
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

public enum YabaImageCompression {
    public static let defaultCompressionPercent: Int = 25

    /// 0...50: higher value = more compression (lower output quality in UI if shown as 100 - value).
    public static var storedCompressionPercent: Int {
        if UserDefaults.standard.object(forKey: Constants.imageCompressionPercentKey) == nil {
            return defaultCompressionPercent
        }
        let v = UserDefaults.standard.integer(forKey: Constants.imageCompressionPercentKey)
        return min(50, max(0, v))
    }

    public static func effectiveQualityPercent(_ stored: Int) -> CGFloat {
        let s = min(50, max(0, stored))
        return CGFloat(100 - s) / 100.0
    }

    /// Re-encodes raster image data for smaller storage, or returns nil if decompression failed.
    public static func compressImageData(_ data: Data) -> Data? {
        #if canImport(UIKit)
        guard let image = UIImage(data: data) else { return nil }
        let q = effectiveQualityPercent(storedCompressionPercent)
        if imageHasAlpha(image) {
            return image.pngData()
        }
        return image.jpegData(compressionQuality: q) ?? data
        #elseif canImport(AppKit)
        guard let image = NSImage(data: data) else { return nil }
        let q = effectiveQualityPercent(storedCompressionPercent)
        if imageHasAlpha(image) {
            return pngData(from: image)
        }
        return jpegData(from: image, compressionQuality: q) ?? data
        #else
        return nil
        #endif
    }

    public static func compressDataPreservingFormat(_ data: Data) -> Data {
        compressImageData(data) ?? data
    }

    #if canImport(UIKit)
    private static func imageHasAlpha(_ image: UIImage) -> Bool {
        guard let cg = image.cgImage else { return false }
        return cgImageHasAlpha(cg)
    }
    #endif

    #if canImport(AppKit)
    private static func imageHasAlpha(_ image: NSImage) -> Bool {
        guard let cg = cgImage(from: image) else { return false }
        return cgImageHasAlpha(cg)
    }

    private static func cgImage(from image: NSImage) -> CGImage? {
        var rect = CGRect(origin: .zero, size: image.size)
        return image.cgImage(forProposedRect: &rect, context: nil, hints: nil)
    }

    private static func pngData(from image: NSImage) -> Data? {
        guard let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff) else { return nil }
        return rep.representation(using: .png, properties: [:])
    }

    private static func jpegData(from image: NSImage, compressionQuality: CGFloat) -> Data? {
        guard let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff) else { return nil }
        return rep.representation(using: .jpeg, properties: [.compressionFactor: compressionQuality])
    }
    #endif

    private static func cgImageHasAlpha(_ cg: CGImage) -> Bool {
        switch cg.alphaInfo {
        case .first, .last, .premultipliedLast, .premultipliedFirst, .alphaOnly:
            return true
        default:
            return false
        }
    }
}
