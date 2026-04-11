//
//  WebPlatform.swift
//  YABACore
//
//  Parity with Compose `WebPlatform` — host surface passed to web shells for theming.
//

import Foundation

public enum WebPlatform: String, Sendable, Codable {
    case compose
    case darwin
}
