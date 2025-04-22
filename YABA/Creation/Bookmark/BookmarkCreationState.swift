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
    var shouldShowFolderSelection: Bool = false
    var shouldShowTagsSelection: Bool = false
    
    func fetchData(with urlString: String) async {
        isLoading = true
        defer {
            isLoading = false
        }
        do {
            if let fetched = try await unfurler.unfurl(urlString: urlString) {
                self.url = fetched.url
                self.host = fetched.host
                self.label = fetched.title
                self.videoUrl = fetched.videoURL
                self.iconData = fetched.iconData
                self.imageData = fetched.imageData
            }
        } catch UnfurlError.cannotCreateURL(let message) {
            
        } catch UnfurlError.unableToUnfurl(let message) {
            
        } catch {
            
        }
    }
    
    func listenUrlChanges(_ perform: @escaping (String) -> Void) {
        debouncedUrl.debounce(for: 0.75, scheduler: RunLoop.main)
            .dropFirst()
            .sink {
                perform($0)
            }
            .store(in: &cancels)
    }
}
