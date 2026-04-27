//
//  DiagramBlockModel.swift
//  YABACore
//

import Foundation

public struct DiagramBlockModel: Sendable, Equatable {
    public var type: String
    public var source: String
    public init(type: String, source: String) {
        self.type = type
        self.source = source
    }
}
