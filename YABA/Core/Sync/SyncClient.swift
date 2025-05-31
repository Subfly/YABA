//
//  SyncClient.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//

import Foundation

final class SyncClient: NSObject {
    private var webSocketTask: URLSessionWebSocketTask?
    private var url: URL?
    private var session: URLSession?
    
    func setUp(with ip: String) {
        self.url = URL(string: "ws://\(ip):\(Constants.port)/sync")!
        self.session = URLSession(configuration: .default)
    }
    
    func connect() {
        guard let url, let session else { return }
        webSocketTask = session.webSocketTask(with: url)
        webSocketTask?.resume()
        
        print("Connecting to \(url)...")
        receiveMessages()
    }
    
    func send(_ message: String) {
        let message = URLSessionWebSocketTask.Message.string(message)
        webSocketTask?.send(message) { error in
            if let error = error {
                print("WebSocket send error: \(error)")
            } else {
                print("Message sent: \(message)")
            }
        }
    }
    
    private func receiveMessages() {
        webSocketTask?.receive { [weak self] result in
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    print("Received text: \(text)")
                case .data(let data):
                    print("Received binary data: \(data)")
                @unknown default:
                    print("Received unknown message")
                }
                self?.receiveMessages()
            case .failure(let error):
                print("WebSocket receive error: \(error)")
            }
        }
    }
    
    func disconnect() {
        webSocketTask?.cancel(with: .goingAway, reason: nil)
        print("Disconnected")
    }
}
