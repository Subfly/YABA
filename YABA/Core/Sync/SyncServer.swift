//
//  SyncServer.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//

import Foundation
import Vapor

enum ServerError: Error {
    case failedToStart
    case failedToStop
}

final class SyncServer {
    private var app: Application?
    private var ip: String? = nil
    
    func setIp(_ ip: String) {
        self.ip = ip
    }
    
    func startServer() async throws {
        do {
            app = try await .make(.production)
            guard let app else { throw ServerError.failedToStart }
            #if DEBUG
            app.logger.logLevel = .debug
            #endif
            
            app.http.server.configuration.address = .hostname(ip, port: Constants.port)
            try await app.startup()
            startListening()
        } catch {
            throw ServerError.failedToStart
        }
    }
    
    func stopServer() async throws {
        do {
            if app?.didShutdown == false {
                try await app?.asyncShutdown()
            }
        } catch {
            throw ServerError.failedToStop
        }
    }
    
    private func startListening() {
        Task {
            guard let app else { return }
            app.webSocket("sync") { req, ws in
                print("WebSocket connected")
                
                ws.onText { ws, text in
                    print("Received text: \(text)")
                    ws.send("Echo: \(text)")
                }
                
                ws.onClose.whenComplete { result in
                    print("WebSocket disconnected: \(result)")
                }
            }
        }
    }
}
