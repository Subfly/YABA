//
//  YabaWebLoadState.swift
//  YABACore
//
//  Parity with Compose `WebLoadState`.
//

import Foundation

public enum YabaWebLoadState: Sendable {
    case idle
    case loading(progressFraction: Float?)
    case pageFinished
    case bridgeReady
    case rendererCrashed
}
