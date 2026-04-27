//
//  MarkdownKMPBlockRevision.swift
//  YABACore
//
//  Port of `blockRenderRevision` from `markdown-renderer/.../BlockRenderer.kt` for SwiftUI
//  invalidation and table row identity.
//

import Foundation
import MarkdownParser

// MARK: - FNV-1a style mix (matches Kotlin)

private let revisionOffsetBasis: Int64 = -3_750_763_034_362_895_579
private let revisionFnvPrime: Int64 = 1_099_511_628_211

private func mixRevision(_ acc: Int64, _ value: Int64) -> Int64 {
    // Match Kotlin `Long` multiply (wraps); plain `*` traps on overflow in Swift.
    (acc ^ value) &* revisionFnvPrime
}

private func revisionHash(_ a: Int64, _ b: Int64) -> Int64 {
    mixRevision(mixRevision(revisionOffsetBasis, a), b)
}

private func revisionHash(_ a: Int64, _ b: Int64, _ c: Int64) -> Int64 {
    mixRevision(revisionHash(a, b), c)
}

private func revisionHash(_ a: Int64, _ b: Int64, _ c: Int64, _ d: Int64) -> Int64 {
    mixRevision(revisionHash(a, b, c), d)
}

private func childRenderRevision(_ node: ContainerNode) -> Int64 {
    var acc = revisionOffsetBasis
    for child in node.children {
        acc = mixRevision(acc, kmpBlockRenderRevision(node: child))
    }
    return acc
}

/// Recomputes a stable revision for the given KMP `Node` (Compose `blockRenderRevision`).
public func kmpBlockRenderRevision(node: Node) -> Int64 {
    if let p = node as? Paragraph {
        return revisionHash(
            Int64(p.lineRange.endLine),
            p.contentHash,
            Int64(p.rawContent?.count ?? 0)
        )
    }
    if let h = node as? Heading {
        return revisionHash(
            Int64(h.level),
            Int64(h.lineRange.endLine),
            h.contentHash,
            Int64(h.rawContent?.count ?? 0)
        )
    }
    if let h = node as? SetextHeading {
        return revisionHash(
            Int64(h.level),
            Int64(h.lineRange.endLine),
            h.contentHash,
            Int64(h.rawContent?.count ?? 0)
        )
    }
    if let n = node as? FencedCodeBlock {
        return revisionHash(
            Int64(n.lineRange.endLine),
            n.contentHash,
            Int64(n.literal.count)
        )
    }
    if let n = node as? IndentedCodeBlock {
        return revisionHash(
            Int64(n.lineRange.endLine),
            n.contentHash,
            Int64(n.literal.count)
        )
    }
    if let n = node as? BlockQuote {
        return revisionHash(
            Int64(n.lineRange.endLine),
            n.contentHash,
            Int64(n.childCount()),
            childRenderRevision(n)
        )
    }
    if let n = node as? ListItem {
        return revisionHash(
            Int64(n.lineRange.endLine),
            n.contentHash,
            Int64(n.childCount()),
            childRenderRevision(n)
        )
    }
    if let n = node as? ListBlock {
        return revisionHash(
            Int64(n.lineRange.endLine),
            n.contentHash,
            Int64(n.childCount()),
            childRenderRevision(n)
        )
    }
    if let n = node as? Table {
        return revisionHash(
            Int64(n.lineRange.endLine),
            n.contentHash,
            Int64(n.childCount()),
            childRenderRevision(n)
        )
    }
    if let n = node as? CustomContainer {
        return revisionHash(
            Int64(n.lineRange.endLine),
            n.contentHash,
            Int64(n.childCount()),
            childRenderRevision(n)
        )
    }
    return revisionHash(Int64(node.lineRange.endLine), node.contentHash)
}

/// Top-level table row / diffable key. Includes index so reordered blocks update correctly.
public func kmpRowIdentifier(index: Int, node: Node) -> String {
    "kmp-\(index)-\(node.stableKey)-\(node.contentHash)"
}
