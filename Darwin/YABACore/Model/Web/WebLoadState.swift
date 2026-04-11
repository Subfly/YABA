//
//  WebLoadState.swift
//  YABACore
//
//  Parity with Compose `WebLoadState`.
//

import Foundation

public enum WebLoadState: Sendable {
    case idle
    case loading(progressFraction: Float?)
    case pageFinished
    case bridgeReady
    case rendererCrashed
}
