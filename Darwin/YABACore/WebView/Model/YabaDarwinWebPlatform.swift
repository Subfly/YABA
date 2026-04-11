//
//  YabaDarwinWebPlatform.swift
//  YABACore
//
//  Parity with Compose `YabaWebPlatform` — host surface passed to web shells for theming.
//

import Foundation

public enum YabaDarwinWebPlatform: String, Sendable, Codable {
    case compose
    case darwin
}
