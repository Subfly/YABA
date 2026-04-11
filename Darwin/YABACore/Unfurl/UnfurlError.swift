//
//  UnfurlError.swift
//  YABACore
//

import Foundation

public enum UnfurlError: Error, Sendable {
    case cannotCreateURL(String)
    case unableToFetchHtml
    case converterBridgeNotReady
    case htmlConversionStartFailed
    case htmlConversionParseFailed
    case htmlConversionTimedOut
}
