//
//  MetadataModels.swift
//  YABACore
//
//  Native metadata extraction result models (non-WebView bridge types).
//

import Foundation

public struct LinkMetadataResult: Sendable, Equatable {
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

public struct PdfMetadataResult: Sendable, Equatable {
    public var title: String?
    public var author: String?
    public var subject: String?
    public var creationDate: String?
    public var pageCount: Int
    public var firstPageImageData: Data?

    public init(
        title: String?,
        author: String?,
        subject: String?,
        creationDate: String?,
        pageCount: Int,
        firstPageImageData: Data?
    ) {
        self.title = title
        self.author = author
        self.subject = subject
        self.creationDate = creationDate
        self.pageCount = pageCount
        self.firstPageImageData = firstPageImageData
    }
}

public struct EpubMetadataResult: Sendable, Equatable {
    public var coverImageData: Data?
    public var coverImageMimeType: String?
    public var title: String?
    public var author: String?
    public var description: String?
    public var pubdate: String?

    public init(
        coverImageData: Data?,
        coverImageMimeType: String?,
        title: String?,
        author: String?,
        description: String?,
        pubdate: String?
    ) {
        self.coverImageData = coverImageData
        self.coverImageMimeType = coverImageMimeType
        self.title = title
        self.author = author
        self.description = description
        self.pubdate = pubdate
    }
}
