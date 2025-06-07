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
import SwiftData

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
    var videoURL: String? = nil
    var imageData: Data? = nil
    var iconData: Data? = nil
    var readableHTML: String? = nil
    
    var selectedType: BookmarkType = .none
    var selectedFolder: YabaCollection? = nil
    var selectedTags: [YabaCollection] = []
    
    var isLoading: Bool = false
    var hasError: Bool = false
    
    var contentAppearance: PreviewContentAppearance = .list
    
    var uncatagroizedFolderCreationRequired: Bool = false
    
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
                self.videoURL = fetched.videoURL
                self.readableHTML = fetched.readableHTML
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
    
    func onDone(
        bookmarkToEdit: YabaBookmark?,
        using modelContext: ModelContext,
        onFinishCallback: @escaping () -> Void
    ) {
        guard let selectedFolder else { return }
        
        withAnimation {
            if let bookmarkToEdit {
                bookmarkToEdit.label = label
                bookmarkToEdit.link = url
                bookmarkToEdit.domain = host
                bookmarkToEdit.bookmarkDescription = description
                bookmarkToEdit.imageUrl = imageURL
                bookmarkToEdit.iconUrl = iconURL
                bookmarkToEdit.videoUrl = videoURL
                bookmarkToEdit.readableHTML = readableHTML
                bookmarkToEdit.type = selectedType.rawValue
                bookmarkToEdit.iconDataHolder = .init(data: iconData)
                bookmarkToEdit.imageDataHolder = .init(data: imageData)
                bookmarkToEdit.editedAt = .now
                bookmarkToEdit.version += 1
                
                // Folder changing
                if let collections = bookmarkToEdit.collections {
                    if let lastSelectedFolderIndex = collections.firstIndex(
                        where: { $0.collectionType == .folder }
                    ) {
                        let lastSelectedFolder = collections[lastSelectedFolderIndex]
                        if lastSelectedFolder.hasChanges(with: selectedFolder) {
                            bookmarkToEdit.collections?.remove(at: lastSelectedFolderIndex)
                            bookmarkToEdit.collections?.append(selectedFolder)
                        }
                    }
                }
                
                // Tags changing
                bookmarkToEdit.collections?.removeAll { $0.collectionType == .tag }
                bookmarkToEdit.collections?.append(contentsOf: selectedTags)
            } else {
                var collections = selectedTags
                collections.append(selectedFolder)
                
                let creationTime: Date = .now
                let newBookmark = YabaBookmark(
                    bookmarkId: UUID().uuidString,
                    link: url,
                    label: label,
                    bookmarkDescription: description,
                    domain: host,
                    createdAt: creationTime,
                    editedAt: creationTime,
                    imageDataHolder: .init(data: imageData),
                    iconDataHolder: .init(data: iconData),
                    imageUrl: imageURL,
                    iconUrl: iconURL,
                    videoUrl: videoURL,
                    readableHTML: readableHTML,
                    type: selectedType,
                    version: 0,
                    collections: collections
                )
                
                modelContext.insert(newBookmark)
            }
            try? modelContext.save()
            onFinishCallback()
        }
    }
    
    func onAppear(
        link: String?,
        bookmarkToEdit: YabaBookmark?,
        collectionToFill: YabaCollection?,
        storedContentAppearance: ViewType,
        storedCardImageSizing: CardViewTypeImageSizing,
        using modelContext: ModelContext
    ) async {
        /// MARK: PREVIEW TYPE SELECTION
        switch storedContentAppearance {
        case .list:
            contentAppearance = .list
        case .card:
            switch storedCardImageSizing {
            case .big:
                contentAppearance = .cardBigImage
            case .small:
                contentAppearance = .cardSmallImage
            }
        }
        
        /// MARK: BOOKMARK FOLDER FILLING
        var folderToFill = bookmarkToEdit?.collections?.first(where: { $0.collectionType == .folder })
        if folderToFill == nil {
            let id = Constants.uncategorizedCollectionId // Thanks #Predicate for that...
            let descriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate<YabaCollection> { collection in
                    collection.collectionId == id
                }
            )
            if let existingUncatagorizedFolder = try? modelContext.fetch(descriptor).first {
                folderToFill = existingUncatagorizedFolder
            } else {
                let creationTime = Date.now
                folderToFill = YabaCollection(
                    collectionId: Constants.uncategorizedCollectionId,
                    label: Constants.uncategorizedCollectionLabelKey,
                    icon: "folder-01",
                    createdAt: creationTime,
                    editedAt: creationTime,
                    color: .none,
                    type: .folder,
                    version: 0,
                )
                uncatagroizedFolderCreationRequired = true
            }
        }
        
        /// MARK: BOOKMARK DATA FILLER FOR VM
        if let bookmarkToEdit {
            url = bookmarkToEdit.link
            label = bookmarkToEdit.label
            description = bookmarkToEdit.bookmarkDescription
            host = bookmarkToEdit.domain
            imageData = bookmarkToEdit.imageDataHolder?.data
            iconData = bookmarkToEdit.iconDataHolder?.data
            videoURL = bookmarkToEdit.videoUrl
            readableHTML = bookmarkToEdit.readableHTML
            selectedType = bookmarkToEdit.bookmarkType
            selectedFolder = bookmarkToEdit.collections?.first(where: { $0.collectionType == .folder })
            selectedTags = bookmarkToEdit.collections?.filter { $0.collectionType == .tag } ?? []
        } else {
            selectedFolder = folderToFill
        }
        
        /// MARK: BOOKMARK URL FILLER FOR SHARE EXTENSIONS
        if let link {
            url = link
        }
        
        /// MARK: COLLECTION FILLING
        if let collectionToFill {
            switch collectionToFill.collectionType {
            case .folder:
                selectedFolder = collectionToFill
            case .tag:
                selectedTags.append(collectionToFill)
            }
        }
    }
    
    func simpleOnAppear(
        link: String?,
        bookmarkToEdit: YabaBookmark?,
        collectionToFill: YabaCollection?,
        using modelContext: ModelContext
    ) async {
        /// MARK: BOOKMARK FOLDER FILLING
        let id = Constants.uncategorizedCollectionId // Thanks #Predicate for that...
        let descriptor = FetchDescriptor<YabaCollection>(
            predicate: #Predicate<YabaCollection> { collection in
                collection.collectionId == id
            }
        )
        
        if let existingUncatagorizedFolder = try? modelContext.fetch(descriptor).first {
            selectedFolder = existingUncatagorizedFolder
        } else {
            let creationTime = Date.now
            selectedFolder = YabaCollection(
                collectionId: Constants.uncategorizedCollectionId,
                label: Constants.uncategorizedCollectionLabelKey,
                icon: "folder-01",
                createdAt: creationTime,
                editedAt: creationTime,
                color: .none,
                type: .folder,
                version: 0
            )
            uncatagroizedFolderCreationRequired = true
        }
        
        /// MARK: BOOKMARK URL FILLER FOR SHARE EXTENSIONS
        if let link {
            url = link
        }
        
        /// MARK: COLLECTION FILLING
        if let collectionToFill {
            switch collectionToFill.collectionType {
            case .folder:
                selectedFolder = collectionToFill
            case .tag:
                selectedTags.append(collectionToFill)
            }
        }
    }
}
