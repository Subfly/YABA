//
//  LinkCleaner.swift
//  YABA
//
//  Created by Ali Taha on 11.08.2025.
//

import Foundation

/// Advanced utility class for aggressive URL cleaning, redirect unwrapping, and site-specific optimizations
class LinkCleaner {
    
    /// Comprehensive set of tracking parameters to remove
    private static let trackingParameters: Set<String> = [
        // Google Analytics & Ads
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
        "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
        "gclid", "gclsrc", "dclid", "wbraid", "gbraid",
        
        // Facebook & Social
        "fbclid", "fb_action_ids", "fb_action_types", "fb_ref", "fb_source",
        "igshid", "igsh", "twclid",
        
        // Microsoft/Bing
        "msclkid", "msclsrc",
        
        // Amazon tracking (but preserve affiliate tags intentionally added)
        "ref", "ref_", "linkCode", "camp", "creative", "creativeASIN",
        "ascsubtag", "asc_campaign", "asc_refurl", "asc_source", "pd_rd_r", "pd_rd_w",
        "pd_rd_wg", "pf_rd_p", "pf_rd_r", "pf_rd_s", "pf_rd_t", "pf_rd_i", "psc",
        
        // Email marketing
        "mc_cid", "mc_eid", "mc_tc",
        
        // HubSpot
        "hsCtaTracking", "hsa_acc", "hsa_ad", "hsa_cam", "hsa_grp", "hsa_kw",
        "hsa_mt", "hsa_net", "hsa_src", "hsa_tgt", "hsa_ver", "_hsenc", "_hsmi",
        "__hssc", "__hstc", "__hsfp",
        
        // LinkedIn
        "li_fat_id", "lipi", "licu",
        
        // TikTok
        "tt_medium", "tt_content",
        
        // Other platforms
        "yclid", "zanpid", "ranMID", "ranEAID", "ranSiteID", "spm", "scm",
        "vn", "vp", "share", "sharesource", "icn", "icp", "icl", "ict", "icm",
        "_openstat", "ito", "itm_source", "itm_medium", "itm_campaign",
        "pk_source", "pk_medium", "pk_campaign", "pk_keyword", "pk_content",
        "mtm_source", "mtm_medium", "mtm_campaign", "mtm_keyword", "mtm_content",
        "nr_email_referer", "vero_conv", "vero_id", "wickedid", "gdfms", "gdftrk",
        "gdffi", "mkt_tok", "trk", "trkCampaign", "sc_campaign", "sc_channel",
        "elqTrackId", "elqTrack", "assetType", "assetId", "recipientId",
        "campaignId", "siteId", "at_medium", "at_campaign", "at_source"
    ]
    
    /// Aggressively cleans and optimizes URLs by removing tracking, unwrapping redirects, and applying site-specific optimizations
    /// - Parameter url: The URL string to clean
    /// - Returns: A cleaned and optimized URL string
    public static func clean(url: String) -> String {
        guard !url.isEmpty else { return url }
        
        // Try to extract URL from text if needed
        let extractedUrl = extractURLFromText(url)
        
        // Parse the URL
        guard var oldUrl = parseURL(extractedUrl) else {
            return url
        }
        
        // Step 1: Unwrap common redirect URLs
        oldUrl = unwrapRedirects(oldUrl)
        
        // Step 2: Apply site-specific cleaning and optimizations
        let newUrl = applySiteSpecificCleaning(oldUrl)
        
        return newUrl.absoluteString
    }
    
    /// Extract URL from text that might contain other content
    private static func extractURLFromText(_ text: String) -> String {
        // Try to extract URL pattern if the input is not a clean URL
        let urlPattern = #"https?://\S+"#
        if let regex = try? NSRegularExpression(pattern: urlPattern, options: []),
           let match = regex.firstMatch(in: text, options: [], range: NSRange(text.startIndex..., in: text)),
           let range = Range(match.range, in: text) {
            return String(text[range])
        }
        return text
    }
    
    /// Parse URL with error handling
    private static func parseURL(_ urlString: String) -> URL? {
        return URL(string: urlString)
    }
    
    /// Unwrap common redirect URLs
    private static func unwrapRedirects(_ url: URL) -> URL {
        let host = url.host?.lowercased() ?? ""
        
        // Facebook redirect unwrapping
        if host == "l.facebook.com", let redirectUrl = url.queryParameter("u") {
            if let unwrapped = URL(string: redirectUrl.removingPercentEncoding ?? redirectUrl) {
                return unwrapped
            }
        }
        
        // Google redirect unwrapping
        if host == "www.google.com" && url.path == "/url",
           let redirectUrl = url.queryParameter("url") {
            if let unwrapped = URL(string: redirectUrl) {
                return unwrapped
            }
        }
        
        // href.li unwrapping
        if host == "href.li" {
            let urlString = url.absoluteString
            if let queryStart = urlString.range(of: "?") {
                let afterQuestion = String(urlString[queryStart.upperBound...])
                if let unwrapped = URL(string: afterQuestion) {
                    return unwrapped
                }
            }
        }
        
        // BusinessWire tracking unwrapping
        if host == "cts.businesswire.com", let redirectUrl = url.queryParameter("url") {
            if let unwrapped = URL(string: redirectUrl) {
                return unwrapped
            }
        }
        
        return url
    }
    
    /// Apply site-specific cleaning and optimizations
    private static func applySiteSpecificCleaning(_ oldUrl: URL) -> URL {
        let host = oldUrl.host?.lowercased() ?? ""
        var newUrl = URLComponents(url: oldUrl, resolvingAgainstBaseURL: false) ?? URLComponents()
        
        // Start with clean slate for query parameters
        newUrl.queryItems = []
        
        // YouTube optimizations
        if host.contains("youtube.com") {
            return cleanYouTubeURL(oldUrl, newUrl: &newUrl)
        }
        
        // Amazon optimizations
        if host.contains("amazon") {
            return cleanAmazonURL(oldUrl, newUrl: &newUrl)
        }
        
        // Facebook optimizations
        if host.contains("facebook.com") {
            return cleanFacebookURL(oldUrl, newUrl: &newUrl)
        }
        
        // Generic cleaning: preserve essential parameters, remove tracking
        return cleanGenericURL(oldUrl, newUrl: &newUrl)
    }
    
    // MARK: - Site-specific cleaning methods
    
    private static func cleanYouTubeURL(_ oldUrl: URL, newUrl: inout URLComponents) -> URL {
        // Preserve video ID
        if let videoId = oldUrl.queryParameter("v") {
            newUrl.queryItems?.append(URLQueryItem(name: "v", value: videoId))
        }
        
        // Preserve timestamp
        if let timestamp = oldUrl.queryParameter("t") {
            newUrl.queryItems?.append(URLQueryItem(name: "t", value: timestamp))
        }
        
        // Preserve playlist
        if let listId = oldUrl.queryParameter("list") {
            newUrl.queryItems?.append(URLQueryItem(name: "list", value: listId))
        }
        
        return newUrl.url ?? oldUrl
    }
    
    private static func cleanAmazonURL(_ oldUrl: URL, newUrl: inout URLComponents) -> URL {
        // Remove www. from Amazon
        newUrl.host = newUrl.host?.replacingOccurrences(of: "www.", with: "")
        
        // Extract product ID and create clean path
        let path = oldUrl.path
        let patterns = [#"\/dp\/(\w+)"#, #"\/product\/(\w+)"#, #"\/d\/(\w+)"#]
        
        for pattern in patterns {
            if let regex = try? NSRegularExpression(pattern: pattern),
               let match = regex.firstMatch(in: path, options: [], range: NSRange(path.startIndex..., in: path)),
               let productIdRange = Range(match.range(at: 1), in: path) {
                let productId = String(path[productIdRange])
                newUrl.path = "/dp/\(productId)"
                break
            }
        }
        
        return newUrl.url ?? oldUrl
    }
    
    private static func cleanFacebookURL(_ oldUrl: URL, newUrl: inout URLComponents) -> URL {
        if oldUrl.path.contains("story.php") {
            if let storyId = oldUrl.queryParameter("story_fbid") {
                newUrl.queryItems?.append(URLQueryItem(name: "story_fbid", value: storyId))
            }
            if let id = oldUrl.queryParameter("id") {
                newUrl.queryItems?.append(URLQueryItem(name: "id", value: id))
            }
        }
        return newUrl.url ?? oldUrl
    }
    
    private static func cleanGenericURL(_ oldUrl: URL, newUrl: inout URLComponents) -> URL {
        // Preserve search query parameter
        if let query = oldUrl.queryParameter("q") {
            newUrl.queryItems?.append(URLQueryItem(name: "q", value: query))
        }
        
        // Remove all tracking parameters, preserve functional ones
        if let queryItems = oldUrl.queryItems {
            for item in queryItems {
                let paramName = item.name.lowercased()
                if !trackingParameters.contains(paramName) && item.name != "q" {
                    // Only add non-tracking parameters that aren't already added
                    if !(newUrl.queryItems?.contains { $0.name == item.name } ?? false) {
                        newUrl.queryItems?.append(item)
                    }
                }
            }
        }
        
        return newUrl.url ?? oldUrl
    }
}

// MARK: - URL Extension for convenience
private extension URL {
    func queryParameter(_ name: String) -> String? {
        guard let components = URLComponents(url: self, resolvingAgainstBaseURL: false),
              let queryItems = components.queryItems else {
            return nil
        }
        return queryItems.first { $0.name == name }?.value
    }
    
    var queryItems: [URLQueryItem]? {
        return URLComponents(url: self, resolvingAgainstBaseURL: false)?.queryItems
    }
}
