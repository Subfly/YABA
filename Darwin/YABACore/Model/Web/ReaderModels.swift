//
//  YabaReaderModels.swift
//  YABACore
//
//  Parity with Compose reader metrics / editor formatting.
//

import Foundation

public struct ReaderMetricsEvent: Sendable, Codable, Equatable {
    public var canCreateAnnotation: Bool
    public var currentPage: Int
    public var pageCount: Int
    public var formatting: EditorFormattingState?

    public init(
        canCreateAnnotation: Bool,
        currentPage: Int,
        pageCount: Int,
        formatting: EditorFormattingState? = nil
    ) {
        self.canCreateAnnotation = canCreateAnnotation
        self.currentPage = currentPage
        self.pageCount = pageCount
        self.formatting = formatting
    }
}

public struct EditorFormattingState: Sendable, Codable, Equatable {
    public var headingLevel: Int
    public var bold: Bool
    public var italic: Bool
    public var underline: Bool
    public var strikethrough: Bool
    public var subscriptEnabled: Bool
    public var superscript: Bool
    public var code: Bool
    public var codeBlock: Bool
    public var blockquote: Bool
    public var bulletList: Bool
    public var orderedList: Bool
    public var taskList: Bool
    public var inlineMath: Bool
    public var blockMath: Bool
    public var canUndo: Bool
    public var canRedo: Bool
    public var canIndent: Bool
    public var canOutdent: Bool
    public var inTable: Bool
    public var canAddRowBefore: Bool
    public var canAddRowAfter: Bool
    public var canDeleteRow: Bool
    public var canAddColumnBefore: Bool
    public var canAddColumnAfter: Bool
    public var canDeleteColumn: Bool
    public var textHighlight: Bool

    public init(
        headingLevel: Int = 0,
        bold: Bool = false,
        italic: Bool = false,
        underline: Bool = false,
        strikethrough: Bool = false,
        subscriptEnabled: Bool = false,
        superscript: Bool = false,
        code: Bool = false,
        codeBlock: Bool = false,
        blockquote: Bool = false,
        bulletList: Bool = false,
        orderedList: Bool = false,
        taskList: Bool = false,
        inlineMath: Bool = false,
        blockMath: Bool = false,
        canUndo: Bool = false,
        canRedo: Bool = false,
        canIndent: Bool = false,
        canOutdent: Bool = false,
        inTable: Bool = false,
        canAddRowBefore: Bool = false,
        canAddRowAfter: Bool = false,
        canDeleteRow: Bool = false,
        canAddColumnBefore: Bool = false,
        canAddColumnAfter: Bool = false,
        canDeleteColumn: Bool = false,
        textHighlight: Bool = false
    ) {
        self.headingLevel = headingLevel
        self.bold = bold
        self.italic = italic
        self.underline = underline
        self.strikethrough = strikethrough
        self.subscriptEnabled = subscriptEnabled
        self.superscript = superscript
        self.code = code
        self.codeBlock = codeBlock
        self.blockquote = blockquote
        self.bulletList = bulletList
        self.orderedList = orderedList
        self.taskList = taskList
        self.inlineMath = inlineMath
        self.blockMath = blockMath
        self.canUndo = canUndo
        self.canRedo = canRedo
        self.canIndent = canIndent
        self.canOutdent = canOutdent
        self.inTable = inTable
        self.canAddRowBefore = canAddRowBefore
        self.canAddRowAfter = canAddRowAfter
        self.canDeleteRow = canDeleteRow
        self.canAddColumnBefore = canAddColumnBefore
        self.canAddColumnAfter = canAddColumnAfter
        self.canDeleteColumn = canDeleteColumn
        self.textHighlight = textHighlight
    }

    enum CodingKeys: String, CodingKey {
        case headingLevel, bold, italic, underline, strikethrough, superscript, code, codeBlock
        case blockquote, bulletList, orderedList, taskList, inlineMath, blockMath
        case canUndo, canRedo, canIndent, canOutdent, inTable
        case canAddRowBefore, canAddRowAfter, canDeleteRow
        case canAddColumnBefore, canAddColumnAfter, canDeleteColumn
        case textHighlight
        case subscriptEnabled = "subscript"
    }
}
