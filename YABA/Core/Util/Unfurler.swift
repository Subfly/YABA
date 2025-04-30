//
//  Unfurler.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import Foundation
import UniformTypeIdentifiers
import LinkPresentation
import OSLog

enum UnfurlError: Error {
    case cannotCreateURL(String)
    case unableToUnfurl(String)
}

@MainActor
class Unfurler {
    private let logger: Logger = .init()
    
    func unfurl(urlString: String) async throws -> YabaLinkPreview? {
        let provider: LPMetadataProvider = .init()
        // Try to create the URL
        guard let url = URL(string: urlString) else {
            self.logger.log(level: .error, "[UNFURLER] Cannot create url for: \(urlString)")
            throw UnfurlError.cannotCreateURL("Can not create the URL from the link")
        }
        
        do {
            let metadata = try await provider.startFetchingMetadata(for: url)
            let icon = try? await self.convertToImage(metadata.iconProvider, forIcon: true)
            let image = try? await self.convertToImage(metadata.imageProvider, forIcon: false)
            return YabaLinkPreview(
                title: metadata.title ?? "",
                url: urlString,
                host: metadata.url?.host() ?? "",
                iconData: icon,
                imageData: image,
                videoURL: metadata.remoteVideoURL?.absoluteString
            )
        } catch {
            throw UnfurlError.unableToUnfurl("Can not load preview for given.")
        }
    }
    
    // Taken from: https://medium.com/@alaputska/creating-custom-link-preview-instead-of-lplinkview-with-swiftui-909512a4cb27
    private func convertToImage(_ imageProvider: NSItemProvider?, forIcon: Bool) async throws -> Data? {
        if let imageProvider {
            let type = if forIcon {
                "public.image"
            } else {
                String(describing: UTType.image)
            }
            
            if imageProvider.hasItemConformingToTypeIdentifier(type) {
                let item = try await imageProvider.loadItem(forTypeIdentifier: type)
                
                if item is UIImage {
                    let image = item as? UIImage
                    return image?.pngData()
                }
                
                if item is URL {
                    guard let url = item as? URL,
                          let data = try? Data(contentsOf: url) else { return nil }
                    
                    return data
                }
                
                if item is Data {
                    guard let data = item as? Data else {
                        return nil
                    }
                    
                    return data
                }
            }
        }
        
        return nil
    }
}
