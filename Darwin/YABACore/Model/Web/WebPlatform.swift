//
//  WebPlatform.swift
//  YABACore
//
//  Parity with Android `WebPlatform` — host surface passed to web shells for theming.
//

import Foundation

public enum WebPlatform: String, Sendable, Codable {
    case android
    case darwin
}
