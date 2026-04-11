//
//  WebShellLoadResult.swift
//  YABACore
//
//  Parity with Compose `WebShellLoadResult`.
//

import Foundation

public enum WebShellLoadResult: String, Sendable, Codable {
    case loaded
    case error
}
