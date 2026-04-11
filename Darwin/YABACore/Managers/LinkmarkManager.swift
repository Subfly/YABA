//
//  LinkmarkManager.swift
//  YABACore
//
//  Link subtype metadata (Compose `LinkmarkManager` parity).
//

import Foundation
import SwiftData

public enum LinkmarkManager {
    public static func queueCreateOrUpdateLinkDetails(
        bookmarkId: String,
        url: String,
        domain: String?,
        videoUrl: String? = nil,
        audioUrl: String? = nil,
        metadataTitle: String? = nil,
        metadataDescription: String? = nil,
        metadataAuthor: String? = nil,
        metadataDate: String? = nil
    ) {
        CoreOperationQueue.shared.queue(name: "CreateOrUpdateLinkDetails:\(bookmarkId)") { context in
            try createOrUpdateLinkDetailsInternal(
                bookmarkId: bookmarkId,
                url: url,
                domain: domain,
                videoUrl: videoUrl,
                audioUrl: audioUrl,
                metadataTitle: metadataTitle,
                metadataDescription: metadataDescription,
                metadataAuthor: metadataAuthor,
                metadataDate: metadataDate,
                context: context
            )
        }
    }

    private static func createOrUpdateLinkDetailsInternal(
        bookmarkId: String,
        url: String,
        domain: String?,
        videoUrl: String?,
        audioUrl: String?,
        metadataTitle: String?,
        metadataDescription: String?,
        metadataAuthor: String?,
        metadataDate: String?,
        context: ModelContext
    ) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let resolvedDomain = domain?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
            ?? Self.extractDomain(from: url)
        let link: LinkBookmarkModel
        if let existing = bookmark.linkDetail {
            link = existing
        } else {
            let inserted = LinkBookmarkModel(url: url, domain: resolvedDomain, bookmark: bookmark)
            context.insert(inserted)
            bookmark.linkDetail = inserted
            link = inserted
        }
        link.url = url
        link.domain = resolvedDomain
        link.videoUrl = videoUrl
        if let audioUrl { link.audioUrl = audioUrl }
        if let metadataTitle { link.metadataTitle = metadataTitle }
        if let metadataDescription { link.metadataDescription = metadataDescription }
        if let metadataAuthor { link.metadataAuthor = metadataAuthor }
        if let metadataDate { link.metadataDate = metadataDate }
        bookmark.editedAt = .now
    }

    /// Public helper for domain resolution (also used when creating link bookmarks).
    public static func extractDomain(from url: String) -> String {
        let withoutProtocol = url._substringAfter("://", default: url)
        let candidate = withoutProtocol._substringBefore("/")
        return candidate._substringBefore("?")._substringBefore("#")
    }
}

private extension String {
    func _substringAfter(_ delimiter: String, default def: String) -> String {
        guard let range = range(of: delimiter) else { return def }
        return String(self[range.upperBound...])
    }

    func _substringBefore(_ delimiter: String) -> String {
        guard let range = range(of: delimiter) else { return self }
        return String(self[..<range.lowerBound])
    }
}
