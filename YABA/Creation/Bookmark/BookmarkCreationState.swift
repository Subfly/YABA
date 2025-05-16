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
internal class BookmarkCreationState {
    @ObservationIgnored
    private let unfurler: Unfurler = .init()
    @ObservationIgnored
    private var debouncedUrl = CurrentValueSubject<String, Never>("")
    @ObservationIgnored
    private var cancels = Set<AnyCancellable>()
    @ObservationIgnored
    var lastFetchedUrl: String = ""
    
    var url: String = "" {
        didSet {
            debouncedUrl.send(url)
        }
    }
    var host: String = ""
    var label: String = ""
    var description: String = ""
    var iconURL: String? = nil
    var imageURL: String? = nil
    var videoUrl: String? = nil
    var imageData: Data? = nil
    var iconData: Data? = nil
    
    var selectedType: BookmarkType = .none
    var selectedFolder: YabaCollection? = nil
    var selectedTags: [YabaCollection] = []
    
    var isLoading: Bool = false
    var hasError: Bool = false
    
    var contentAppearance: PreviewContentAppearance = .list
    
    let toastManager: ToastManager = .init()
    
    @ObservationIgnored
    let isInEditMode: Bool
    
    init(isInEditMode: Bool) {
        self.isInEditMode = isInEditMode
    }
    
    func fetchData(with urlString: String) async {
        if urlString.isEmpty || urlString == lastFetchedUrl {
            return
        }
        
        isLoading = true
        defer {
            isLoading = false
        }
        
        do {
            if let fetched = try await unfurler.unfurl(urlString: urlString) {
                self.host = fetched.host ?? ""
                if label.isEmpty {
                    self.label = fetched.title ?? ""
                }
                if description.isEmpty {
                    self.description = fetched.description ?? ""
                }
                self.iconURL = fetched.iconURL
                self.imageURL = fetched.imageURL
                self.videoUrl = fetched.videoURL
                self.iconData = fetched.iconData
                self.imageData = fetched.imageData
            }
            hasError = false
            lastFetchedUrl = urlString
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Success Text"),
                accentColor: .green,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .success,
                onAcceptPressed: { self.toastManager.hide() }
            )
        } catch UnfurlError.cannotCreateURL(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
            hasError = true
        } catch UnfurlError.unableToUnfurl(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
            hasError = true
        } catch {
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Error Text"),
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
            hasError = true
        }
    }
    
    func listenUrlChanges(_ perform: @escaping (String) -> Void) {
        debouncedUrl.debounce(for: 0.75, scheduler: RunLoop.main)
            .dropFirst(isInEditMode ? 1 : 0)
            .sink { perform($0) }
            .store(in: &cancels)
    }
}
