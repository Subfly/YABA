//
//  YabaBaseObservableState.swift
//  YABACore
//

import Foundation
import Observation

@Observable
@MainActor
open class YabaBaseObservableState<State: Sendable> {
    public private(set) var state: State

    public init(initialState: State) {
        self.state = initialState
    }

    public func apply(_ update: (inout State) -> Void) {
        update(&state)
    }

    public func replaceState(_ newState: State) {
        state = newState
    }
}
