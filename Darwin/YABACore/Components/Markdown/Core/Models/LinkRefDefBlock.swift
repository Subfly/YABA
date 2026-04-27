//
//  LinkRefDefBlock.swift
//  YABACore
//

import Foundation

public struct LinkRefDefBlock: Sendable, Equatable {
    public var label: String
    public var destination: String
    public var title: String?
    public init(label: String, destination: String, title: String?) {
        self.label = label
        self.destination = destination
        self.title = title
    }
}
