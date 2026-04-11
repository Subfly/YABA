//
//  YabaDarwinWebLoadState.swift
//  YABACore
//
//  Parity with Compose `WebLoadState`.
//

import Foundation

public enum YabaDarwinWebLoadState: Sendable {
    case idle
    case loading(progressFraction: Float?)
    case pageFinished
    case bridgeReady
    case rendererCrashed
}
