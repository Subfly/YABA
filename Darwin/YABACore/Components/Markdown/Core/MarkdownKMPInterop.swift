//
//  MarkdownKMPInterop.swift
//  YABACore
//
//  Helpers for values exported by the Kotlin/Native MarkdownParser framework.
//

import Foundation
import MarkdownParser

public typealias KMPStringDict = [String: String]

enum MarkdownKMPInterop {
    static func stringDict(from value: [String: String]?) -> KMPStringDict {
        value ?? [:]
    }

    static func optionalInt32(from value: KotlinInt?) -> Int? {
        guard let value else { return nil }
        return Int(value.int32Value)
    }

    static func intRangeList(from ranges: [KotlinIntRange]?) -> [ClosedRange<Int>] {
        guard let ranges else { return [] }
        return ranges.compactMap { r in
            let a = Int(r.start.int32Value)
            let b = Int(r.endInclusive.int32Value)
            return a...b
        }
    }
}
