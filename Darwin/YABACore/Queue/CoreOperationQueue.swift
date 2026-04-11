//
//  CoreOperationQueue.swift
//  YABACore
//
//  FIFO serialized persistence work, analogous to Compose `CoreOperationQueue`.
//  Each job receives its own `ModelContext` from `CoreStore` and must leave the store
//  consistent; the queue finishes with an explicit `save()` when `hasChanges`.
//

import Foundation
import SwiftData

/// Serializes SwiftData mutations so ordering matches call order and races are avoided.
public final class CoreOperationQueue: @unchecked Sendable {
    public static let shared = CoreOperationQueue()

    private let queue = DispatchQueue(label: "dev.subfly.yaba.core.operation", qos: .userInitiated)
    private let lock = NSLock()
    private var _pendingCount: Int = 0
    private var _currentName: String?

    private init() {}

    /// Number of operations waiting or running (best-effort, for diagnostics).
    public var pendingCount: Int {
        lock.lock()
        defer { lock.unlock() }
        return _pendingCount
    }

    /// Currently executing operation name, if any.
    public var currentOperationName: String? {
        lock.lock()
        defer { lock.unlock() }
        return _currentName
    }

    /// Start is a no-op (Compose starts explicitly); kept for API symmetry.
    public func start() {}

    /// Enqueue a synchronous mutation; completion runs on an arbitrary queue.
    public func queue(
        name: String,
        operation: @escaping (ModelContext) throws -> Void,
        completion: ((Error?) -> Void)? = nil
    ) {
        lock.lock()
        _pendingCount += 1
        lock.unlock()

        queue.async { [weak self] in
            guard let self else { return }
            self.lock.lock()
            self._currentName = name
            self.lock.unlock()

            defer {
                self.lock.lock()
                self._currentName = nil
                self._pendingCount = max(0, self._pendingCount - 1)
                self.lock.unlock()
            }

            do {
                let context = try CoreStore.makeWriteContext()
                try operation(context)
                try CoreStore.save(context)
                completion?(nil)
            } catch {
                completion?(error)
            }
        }
    }

    /// Enqueue and await completion (async wrapper around the serial queue).
    public func queueAndAwait(
        name: String,
        operation: @escaping (ModelContext) throws -> Void
    ) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            queue(name: name, operation: operation) { error in
                if let error {
                    cont.resume(throwing: error)
                } else {
                    cont.resume()
                }
            }
        }
    }
}
