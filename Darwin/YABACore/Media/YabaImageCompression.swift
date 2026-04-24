//
//  YabaImageCompression.swift
//  YABACore
//
//  Parity with Compose `ImageCompression`: maps stored 0...50 to effective quality 100...50.
//

import Foundation
import UIKit

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
        guard let image = UIImage(data: data) else { return nil }
        let q = effectiveQualityPercent(storedCompressionPercent)
        if imageHasAlpha(image) {
            return image.pngData()
        }
        return image.jpegData(compressionQuality: q) ?? data
    }

    public static func compressDataPreservingFormat(_ data: Data) -> Data {
        compressImageData(data) ?? data
    }

    private static func imageHasAlpha(_ image: UIImage) -> Bool {
        guard let cg = image.cgImage else { return false }
        switch cg.alphaInfo {
        case .first, .last, .premultipliedLast, .premultipliedFirst, .alphaOnly:
            return true
        default:
            return false
        }
    }
}
