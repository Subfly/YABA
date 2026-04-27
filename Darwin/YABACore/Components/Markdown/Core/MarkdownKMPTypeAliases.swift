//
//  MarkdownKMPTypeAliases.swift
//  YABACore
//
//  The `MarkdownParser` module exposes a top-level `Table` AST type, but the module also
//  defines a `MarkdownParser` class—so `MarkdownParser.Table` is resolved as a nested type
//  on that class (and fails). Importing `SwiftUI` adds another `Table`. This file imports
//  only `MarkdownParser`, so `Table` here unambiguously names the KMP node.
//

import MarkdownParser

/// KMP GFM table block (not `SwiftUI.Table`).
public typealias MarkdownKMPTableNode = Table
