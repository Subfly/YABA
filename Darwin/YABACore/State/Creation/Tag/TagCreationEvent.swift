//
//  TagCreationEvent.swift
//  YABACore
//
//  Parity with Compose `TagCreationEvent` — color as `YabaCoreColorRole` (Compose `YabaColor.code`).
//

import Foundation

public enum TagCreationEvent: Sendable {
    case onInitWithTag(tagId: String?)
    case onSelectNewColor(YabaCoreColorRole)
    case onSelectNewIcon(String)
    case onChangeLabel(String)
    case onSave
}
