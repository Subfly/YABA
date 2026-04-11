//
//  DropZone.swift
//  YABACore
//
//  Parity with Compose `DropZone`.
//

import Foundation

public enum DropZone: String, Sendable, Codable, CaseIterable {
    case top
    case bottom
    case middle
    case none
}
