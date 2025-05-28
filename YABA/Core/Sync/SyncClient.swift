//
//  SyncClient.swift
//  YABA
//
//  Created by Ali Taha on 26.05.2025.
//

import Foundation

enum SyncError: Error {
    case couldNotConnect
}

final class SyncClient {
    private var webSocket: URLSessionWebSocketTask?

    func connect(to payload: SyncQRCodePayload) throws {
        guard let url = URL(string: "ws://\(payload.ipAddress):\(payload.port)/sync") else {
            throw SyncError.couldNotConnect
        }
        let session = URLSession(configuration: .default)
        webSocket = session.webSocketTask(with: url)
        webSocket?.resume()

        listen()
    }

    private func listen() {
        webSocket?.receive { [weak self] result in
            switch result {
            case .failure(let error):
                print("WebSocket failed: \(error)")
            case .success(let message):
                // Handle sync message
                print("Received: \(message)")
                self?.listen()
            }
        }
    }

    func send(data: Data) {
        webSocket?.send(.data(data)) { error in
            if let error = error {
                print("Send error: \(error)")
            }
        }
    }

    func disconnect() {
        webSocket?.cancel(with: .goingAway, reason: nil)
    }
}
