//
//  LinkCleaner.swift
//  YABACore
//
//  Rule-based URL cleanup (tracking params, fragments) with an extension-friendly pipeline.
//

import Foundation

/// A single transformation step in URL cleanup. Steps run in order.
public struct LinkCleaningStep: Sendable {
    public let id: String
    private let _apply: @Sendable (inout URLComponents) -> Void

    public init(id: String, apply: @escaping @Sendable (inout URLComponents) -> Void) {
        self.id = id
        self._apply = apply
    }

    public func apply(to components: inout URLComponents) {
        _apply(&components)
    }
}

public enum LinkCleaner {
    public static let defaultSteps: [LinkCleaningStep] = [
        LinkCleaningStep(id: "strip.fragment") { c in
            c.fragment = nil
        },
        LinkCleaningStep(id: "normalize.host.lowercase") { c in
            if let host = c.host {
                c.host = host.lowercased()
            }
        },
        LinkCleaningStep(id: "normalize.scheme.lowercase") { c in
            if let scheme = c.scheme {
                c.scheme = scheme.lowercased()
            }
        },
        LinkCleaningStep(id: "strip.tracking.query") { c in
            guard var items = c.queryItems, !items.isEmpty else { return }
            let blockedExact: Set<String> = [
                "fbclid", "gclid", "igshid", "mc_eid", "mc_cid", "ref_src", "_ga",
                "spm", "ved", "si", "mkt_tok",
            ]
            let blockedPrefixes = ["utm_"]
            items = items.filter { item in
                let name = item.name.lowercased()
                if blockedExact.contains(name) { return false }
                if blockedPrefixes.contains(where: { name.hasPrefix($0) }) { return false }
                return true
            }
            c.percentEncodedQuery = nil
            c.queryItems = items.isEmpty ? nil : items
        },
    ]

    public static func clean(_ raw: String, extraSteps: [LinkCleaningStep] = []) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return trimmed }

        guard let url = URL(string: trimmed),
              let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https",
              var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        else {
            return trimmed
        }

        var steps = defaultSteps
        steps.append(contentsOf: extraSteps)
        for step in steps {
            step.apply(to: &components)
        }

        if let s = components.string, !s.isEmpty {
            return s
        }
        return trimmed
    }
}
