//
//  AnnotationCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class AnnotationCreationStateMachine: YabaBaseObservableState<AnnotationCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: AnnotationCreationUIState = AnnotationCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: AnnotationCreationEvent) async {
        switch event {
        case let .onInitWithSelection(draft):
            apply {
                $0.bookmarkId = draft.bookmarkId
                $0.readableVersionId = draft.readableVersionId
                $0.quoteText = draft.quoteText
                $0.extrasJson = draft.extrasJson
                $0.annotationId = nil
            }
        case let .onInitWithAnnotation(bookmarkId, annotationId):
            apply {
                $0.bookmarkId = bookmarkId
                $0.annotationId = annotationId
            }
        case let .onSelectNewColor(c):
            apply { $0.colorRole = c }
        case let .onChangeNote(note):
            apply { $0.note = note }
        case let .onChangeQuote(quote):
            apply { $0.quoteText = quote }
        case .onSave:
            let bid = state.bookmarkId
            guard !bid.isEmpty else {
                apply { $0.lastError = "bookmarkId required" }
                return
            }
            apply { $0.lastError = nil }
            let colorRaw = state.colorRole.rawValue
            if let aid = state.annotationId {
                AnnotationManager.queueUpdateAnnotation(annotationId: aid, colorRoleRaw: colorRaw, note: state.note.nilIfEmpty)
            } else {
                AnnotationManager.queueInsertAnnotation(
                    bookmarkId: bid,
                    readableVersionId: state.readableVersionId,
                    type: state.annotationType,
                    colorRoleRaw: colorRaw,
                    note: state.note.nilIfEmpty,
                    quoteText: state.quoteText,
                    extrasJson: state.extrasJson
                )
            }
        case .onDelete:
            if let aid = state.annotationId {
                AnnotationManager.queueDeleteAnnotation(annotationId: aid)
            }
        case let .create(bookmarkId, rv, type, note, quote):
            AnnotationManager.queueInsertAnnotation(
                bookmarkId: bookmarkId,
                readableVersionId: rv,
                type: type,
                note: note,
                quoteText: quote
            )
        }
    }
}
