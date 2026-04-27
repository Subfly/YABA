//
//  MarkdownKMPStringDictBridge.swift
//  YABACore
//
//  ObjC dictionary → `[String: String]` (e.g. fenced `attributes.pairs`).
//

import Foundation

enum MarkdownKMPStringDictBridge {
    static func dict(_ pairs: Any?) -> [String: String] {
        if let d = pairs as? [String: String] { return d }
        if let n = pairs as? NSDictionary {
            var o: [String: String] = [:]
            n.enumerateKeysAndObjects { k, v, _ in
                if let ks = k as? String, let vs = v as? String { o[ks] = vs }
            }
            return o
        }
        return [:]
    }
}
