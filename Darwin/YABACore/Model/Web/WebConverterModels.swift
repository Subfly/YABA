//
//  YabaWebConverterModels.swift
//  YABACore
//
//  Parity with Compose `WebConverterModels.kt`.
//

import Foundation

/// Asset placeholder from converter output (e.g. `yaba-asset://N` → remote URL).
public struct WebConverterAsset: Sendable, Equatable {
    public var placeholder: String
    public var url: String
    public var alt: String?

    public init(placeholder: String, url: String, alt: String? = nil) {
        self.placeholder = placeholder
        self.url = url
        self.alt = alt
    }
}

/// Link metadata from the WebView converter bridge (web-meta-scraper + tidy-url on Core-fetched HTML).
public struct WebLinkMetadata: Sendable, Equatable {
    public var cleanedUrl: String
    public var title: String?
    public var description: String?
    public var author: String?
    public var date: String?
    public var audio: String?
    public var video: String?
    public var image: String?
    public var logo: String?

    public init(
        cleanedUrl: String,
        title: String? = nil,
        description: String? = nil,
        author: String? = nil,
        date: String? = nil,
        audio: String? = nil,
        video: String? = nil,
        image: String? = nil,
        logo: String? = nil
    ) {
        self.cleanedUrl = cleanedUrl
        self.title = title
        self.description = description
        self.author = author
        self.date = date
        self.audio = audio
        self.video = video
        self.image = image
        self.logo = logo
    }
}

/// Converter bridge result before [ConverterResultProcessor] runs (Markdown body + assets + metadata).
public struct WebConverterResult: Sendable, Equatable {
    public var markdown: String
    public var assets: [WebConverterAsset]
    public var linkMetadata: WebLinkMetadata

    public init(
        markdown: String,
        assets: [WebConverterAsset],
        linkMetadata: WebLinkMetadata
    ) {
        self.markdown = markdown
        self.assets = assets
        self.linkMetadata = linkMetadata
    }
}
