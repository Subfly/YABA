//
//  BookmarkCreationState.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//
//  Debounce logic taken from: https://stackoverflow.com/questions/76887491/swift-observation-framework-and-debounce

import Foundation
import SwiftUI
import Combine

@MainActor
@Observable
class BookmarkCreationState {
    @ObservationIgnored
    private let unfurler: Unfurler = .init()
    
    @ObservationIgnored
    private var debouncedUrl = CurrentValueSubject<String, Never>("")
    @ObservationIgnored
    private var cancels = Set<AnyCancellable>()
    
    var url: String = "" {
        didSet {
            debouncedUrl.send(url)
        }
    }
    var host: String = ""
    var label: String = ""
    var description: String = ""
    var videoUrl: String? = nil
    var imageData: Data? = nil
    var iconData: Data? = nil
    
    var selectedType: BookmarkType = .none
    var selectedFolder: YabaCollection? = nil
    var selectedTags: [YabaCollection] = []
    
    var isLoading: Bool = false
    var hasError: Bool = false
    
    let toastManager: ToastManager = .init()
    
    func fetchData(with urlString: String) async {
        if urlString.isEmpty {
            return
        }
        
        isLoading = true
        defer {
            isLoading = false
        }
        
        do {
            if let fetched = try await unfurler.unfurl(urlString: urlString) {
                self.host = fetched.host
                self.label = fetched.title
                self.videoUrl = fetched.videoURL
                self.iconData = fetched.iconData
                self.imageData = fetched.imageData
            }
            hasError = false
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Success Text"),
                accentColor: .green,
                iconType: .success,
            )
        } catch UnfurlError.cannotCreateURL(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                iconType: .error,
            )
            hasError = true
        } catch UnfurlError.unableToUnfurl(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                iconType: .error,
            )
            hasError = true
        } catch {
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Error Text"),
                accentColor: .red,
                iconType: .error,
            )
            hasError = true
        }
    }
    
    func listenUrlChanges(_ perform: @escaping (String) -> Void) {
        debouncedUrl.debounce(for: 0.75, scheduler: RunLoop.main)
            .sink { perform($0) }
            .store(in: &cancels)
    }
}
