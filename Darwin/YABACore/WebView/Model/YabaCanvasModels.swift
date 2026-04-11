//
//  YabaCanvasModels.swift
//  YABACore
//
//  Parity with Compose `CanvasHostMetrics` / `CanvasHostStyleState`.
//

import Foundation

public struct YabaCanvasHostMetrics: Sendable, Codable, Equatable {
    public var activeTool: String
    public var hasSelection: Bool
    public var canUndo: Bool
    public var canRedo: Bool
    public var gridModeEnabled: Bool
    public var objectsSnapModeEnabled: Bool

    public init(
        activeTool: String = "selection",
        hasSelection: Bool = false,
        canUndo: Bool = false,
        canRedo: Bool = false,
        gridModeEnabled: Bool = true,
        objectsSnapModeEnabled: Bool = true
    ) {
        self.activeTool = activeTool
        self.hasSelection = hasSelection
        self.canUndo = canUndo
        self.canRedo = canRedo
        self.gridModeEnabled = gridModeEnabled
        self.objectsSnapModeEnabled = objectsSnapModeEnabled
    }
}

public struct YabaCanvasHostStyleState: Sendable, Codable, Equatable {
    public var hasSelection: Bool
    public var selectionCount: Int
    public var selectionElementTypes: [String]
    public var primaryElementType: String
    public var elementTypeMixed: Bool
    public var availableOptionGroups: [String]
    public var strokeYabaCode: Int
    public var backgroundYabaCode: Int
    public var strokeWidthKey: String
    public var strokeStyle: String
    public var roughnessKey: String
    public var edgeKey: String
    public var fontSizeKey: String
    public var opacityStep: Int
    public var mixedStroke: Bool
    public var mixedBackground: Bool
    public var mixedStrokeWidth: Bool
    public var mixedStrokeStyle: Bool
    public var mixedRoughness: Bool
    public var mixedEdge: Bool
    public var mixedFontSize: Bool
    public var mixedOpacity: Bool
    public var arrowTypeKey: String
    public var mixedArrowType: Bool
    public var startArrowheadKey: String
    public var endArrowheadKey: String
    public var mixedStartArrowhead: Bool
    public var mixedEndArrowhead: Bool
    public var availableStartArrowheads: [String]
    public var availableEndArrowheads: [String]
    public var fillStyleKey: String
    public var mixedFillStyle: Bool

    public init(
        hasSelection: Bool = false,
        selectionCount: Int = 0,
        selectionElementTypes: [String] = [],
        primaryElementType: String = "",
        elementTypeMixed: Bool = false,
        availableOptionGroups: [String] = [],
        strokeYabaCode: Int = 0,
        backgroundYabaCode: Int = 0,
        strokeWidthKey: String = "thin",
        strokeStyle: String = "solid",
        roughnessKey: String = "architect",
        edgeKey: String = "sharp",
        fontSizeKey: String = "M",
        opacityStep: Int = 10,
        mixedStroke: Bool = false,
        mixedBackground: Bool = false,
        mixedStrokeWidth: Bool = false,
        mixedStrokeStyle: Bool = false,
        mixedRoughness: Bool = false,
        mixedEdge: Bool = false,
        mixedFontSize: Bool = false,
        mixedOpacity: Bool = false,
        arrowTypeKey: String = "sharp",
        mixedArrowType: Bool = false,
        startArrowheadKey: String = "none",
        endArrowheadKey: String = "none",
        mixedStartArrowhead: Bool = false,
        mixedEndArrowhead: Bool = false,
        availableStartArrowheads: [String] = [],
        availableEndArrowheads: [String] = [],
        fillStyleKey: String = "solid",
        mixedFillStyle: Bool = false
    ) {
        self.hasSelection = hasSelection
        self.selectionCount = selectionCount
        self.selectionElementTypes = selectionElementTypes
        self.primaryElementType = primaryElementType
        self.elementTypeMixed = elementTypeMixed
        self.availableOptionGroups = availableOptionGroups
        self.strokeYabaCode = strokeYabaCode
        self.backgroundYabaCode = backgroundYabaCode
        self.strokeWidthKey = strokeWidthKey
        self.strokeStyle = strokeStyle
        self.roughnessKey = roughnessKey
        self.edgeKey = edgeKey
        self.fontSizeKey = fontSizeKey
        self.opacityStep = opacityStep
        self.mixedStroke = mixedStroke
        self.mixedBackground = mixedBackground
        self.mixedStrokeWidth = mixedStrokeWidth
        self.mixedStrokeStyle = mixedStrokeStyle
        self.mixedRoughness = mixedRoughness
        self.mixedEdge = mixedEdge
        self.mixedFontSize = mixedFontSize
        self.mixedOpacity = mixedOpacity
        self.arrowTypeKey = arrowTypeKey
        self.mixedArrowType = mixedArrowType
        self.startArrowheadKey = startArrowheadKey
        self.endArrowheadKey = endArrowheadKey
        self.mixedStartArrowhead = mixedStartArrowhead
        self.mixedEndArrowhead = mixedEndArrowhead
        self.availableStartArrowheads = availableStartArrowheads
        self.availableEndArrowheads = availableEndArrowheads
        self.fillStyleKey = fillStyleKey
        self.mixedFillStyle = mixedFillStyle
    }
}
