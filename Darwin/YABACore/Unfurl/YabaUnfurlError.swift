//
//  YabaUnfurlError.swift
//  YABACore
//

import Foundation

public enum YabaUnfurlError: Error, Sendable {
    case cannotCreateURL(String)
    case unableToFetchHtml
    case converterBridgeNotReady
    case htmlConversionStartFailed
    case htmlConversionParseFailed
    case htmlConversionTimedOut
}
