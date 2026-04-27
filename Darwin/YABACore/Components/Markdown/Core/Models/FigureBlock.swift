//
//  FigureBlock.swift
//  YABACore
//

import Foundation

public struct FigureBlock: Sendable, Equatable {
    public var imageURL: String
    public var caption: String
    public var width: Int?
    public var height: Int?
    public var attributes: KMPStringDict
    public init(imageURL: String, caption: String, width: Int?, height: Int?, attributes: KMPStringDict) {
        self.imageURL = imageURL
        self.caption = caption
        self.width = width
        self.height = height
        self.attributes = attributes
    }
}
