//
//  YabaScreenStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public protocol YabaScreenStateMachine<Event>: AnyObject {
    associatedtype Event: Sendable
    func send(_ event: Event) async
}
