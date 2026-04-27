//
//  MarkdownEnvironment.swift
//  YABACore
//
//  Environment keys and view helpers for markdown preview.
//

import SwiftUI

private struct MarkdownEnvironmentKey: EnvironmentKey {
    static let defaultValue: MarkdownThemeTokens? = nil
}

private struct MarkdownPreviewConfigurationKey: EnvironmentKey {
    static let defaultValue: MarkdownPreviewConfiguration = .init()
}

public struct MarkdownTocHeadingsKey: EnvironmentKey {
    public static let defaultValue: [MarkdownTocHeading] = []
}

extension EnvironmentValues {
    public var markdownTheme: MarkdownThemeTokens? {
        get { self[MarkdownEnvironmentKey.self] }
        set { self[MarkdownEnvironmentKey.self] = newValue }
    }

    public var markdownPreviewConfiguration: MarkdownPreviewConfiguration {
        get { self[MarkdownPreviewConfigurationKey.self] }
        set { self[MarkdownPreviewConfigurationKey.self] = newValue }
    }

    /// Collected from the current KMP `Document` for `[TOC]` / `TocPlaceholder` resolution.
    public var markdownTocHeadings: [MarkdownTocHeading] {
        get { self[MarkdownTocHeadingsKey.self] }
        set { self[MarkdownTocHeadingsKey.self] = newValue }
    }
}

public extension View {
    func markdownTheme(_ theme: MarkdownThemeTokens?) -> some View {
        environment(\.markdownTheme, theme)
    }

    func markdownPreviewConfiguration(_ config: MarkdownPreviewConfiguration) -> some View {
        environment(\.markdownPreviewConfiguration, config)
    }

    func markdownTocHeadings(_ headings: [MarkdownTocHeading]) -> some View {
        environment(\.markdownTocHeadings, headings)
    }
}
