# Darwin Platform - YABA

The Apple platform implementation of YABA, supporting iOS, macOS, and visionOS with native SwiftUI and SwiftData, built with privacy and offline-first principles.

## 🍎 Platform Support

- **iOS**: 17.0+ (iPhone, iPad)
- **macOS**: 14.0+ (Mac)
- **visionOS**: 1.0+ (Apple Vision Pro)

## 📁 Project Structure

```
Darwin/
├── YABA/                    # Main Application
│   ├── Core/               # Core functionality and models
│   │   ├── App/           # App-level components
│   │   ├── Components/    # Reusable UI components
│   │   ├── Data/          # Data management and persistence
│   │   ├── Model/         # Data models and schemas
│   │   ├── Navigation/    # Navigation logic
│   │   ├── Sync/          # Synchronization logic
│   │   ├── Unfurl/        # Link preview functionality
│   │   └── Util/          # Utility classes and extensions
│   ├── Creation/          # Bookmark and collection creation
│   ├── Detail/            # Detail views for bookmarks and collections
│   ├── Home/              # Main home screen and components
│   ├── Onboarding/        # User onboarding flow
│   ├── Search/            # Search functionality
│   ├── Settings/          # App settings and configuration
│   ├── Sync/              # Data synchronization
│   └── Assets.xcassets/   # App assets and resources
├── YABAShare/             # iOS Share Extension
├── YABAShareMac/          # macOS Share Extension
├── YABAStatusMenuItem/    # macOS Status Menu Item
├── YABA.xcodeproj/        # Xcode project file
└── buildServer.json       # Build server configuration
```

## 🏗️ Architecture

### Design Patterns
- **MV Architecture**: Model-View pattern with SwiftUI
- **Observable Pattern**: Using `@Observable` for state management
- **Repository Pattern**: Data access abstraction through SwiftData
- **Factory Pattern**: Component creation and configuration
- **Strategy Pattern**: Platform-specific implementations

### State Management
```swift
// Observable state classes
@Observable
class BookmarkCreationState {
    var url: String = ""
    var label: String = ""
    var isLoading: Bool = false
    // ... other properties
}

// Environment-based state sharing
@Observable
class AppState {
    var selectedCollection: YabaCollection?
    var selectedBookmark: YabaBookmark?
}
```

### Data Flow
```
User Action → SwiftUI View → @Observable State → Business Logic → SwiftData → Local Storage
```

## 🛠️ Technology Stack

### Core Technologies
- **SwiftUI**: Modern declarative UI framework
- **SwiftData**: Local data persistence
- **Combine**: Reactive programming for async operations
- **Swift Concurrency**: async/await for concurrent operations

### Key Frameworks
- **UniformTypeIdentifiers**: File type handling
- **UserNotifications**: Local notifications
- **TipKit**: In-app guidance and tips

### Build & Development
- **Xcode**: IDE and build system
- **Swift Package Manager**: Dependency management

## 📱 Features

### Core Functionality
- **Bookmark Management**: Create, edit, delete, and organize bookmarks
- **Collections**: Folders and tags for organization
- **Search**: Full-text search with Spotlight integration
- **Import/Export**: JSON, CSV, and HTML format support
- **Offline First**: All functionality works without internet connection

### Platform-Specific Features

#### iOS
- Native iOS design language
- Share extension for quick bookmarking
- Spotlight search integration
- Widget support (planned)
- Shortcuts integration (planned)

#### macOS
- Native macOS design language
- Status menu integration
- Keyboard shortcuts

#### visionOS
- Spatial computing interface
- Immersive experiences

### Advanced Features
- **Link Previews**: Automatic metadata extraction
- **Reader Mode**: Distraction-free reading
- **Reminders**: Local notification reminders
- **Deep Linking**: Custom URL scheme support
- **Privacy Focused**: No data collection, no tracking, no analytics
- **Local Backup**: Local backup options

## 🚀 Getting Started

### Prerequisites
- Xcode 15.0+
- iOS 17.0+, macOS 14.0+, visionOS 1.0+
- Apple Developer Account (for distribution)

### Development Setup

1. **Open the project**:
   ```bash
   cd Darwin
   open YABA.xcodeproj
   ```

2. **Configure signing**:
   - Select your development team
   - Update bundle identifiers if needed

3. **Build and run**:
   - Select target device/simulator
   - Press Cmd+R to build and run

### Configuration

#### Deep Linking
1. Configure custom URL scheme in Info.plist
2. Handle deep links in `DeepLinkManager`

#### Share Extensions
1. Configure app groups for data sharing
2. Set up entitlements for extensions

## 📦 Building & Distribution

### Development Build
```bash
xcodebuild -project YABA.xcodeproj -scheme YABA -configuration Debug
```

### Release Build
```bash
xcodebuild -project YABA.xcodeproj -scheme YABA -configuration Release
```

### Archive for Distribution
```bash
xcodebuild -project YABA.xcodeproj -scheme YABA -configuration Release archive
```

### Targets
- **YABA**: Main application
- **YABAShare**: iOS share extension
- **YABAShareMac**: macOS share extension
- **YABAStatusMenuItem**: macOS status menu item

## 🔧 Configuration

### Build Settings
- **Debug**: Development features, verbose logging
- **Release**: Production optimizations, minimal logging
- **TestFlight**: Beta testing configuration

### Entitlements
- App groups for extensions
- Network access
- File access

## 🐛 Debugging

### Common Issues
1. **Share Extension Problems**: Verify app group settings
2. **Build Errors**: Clean build folder and rebuild

### Debug Tools
- Xcode Instruments for performance profiling
- Console app for system logs

## 📚 Code Examples

### Creating a Bookmark
```swift
@Observable
class BookmarkCreationState {
    func createBookmark(using context: ModelContext) {
        let bookmark = YabaBookmark()
        bookmark.label = label
        bookmark.link = url
        bookmark.domain = extractDomain(from: url)
        
        context.insert(bookmark)
        try? context.save()
    }
}
```

### State Management
```swift
struct HomeView: View {
    @State private var homeState: HomeState = .init()
    @Environment(\.appState) private var appState
    
    var body: some View {
        NavigationView {
            // View content
        }
        .onChange(of: homeState.selectedBookmark) { _, bookmark in
            appState.selectedBookmark = bookmark
        }
    }
}
```

### Data Persistence
```swift
@Query(sort: \YabaBookmark.createdAt, order: .reverse)
private var bookmarks: [YabaBookmark]

@Query(filter: #Predicate<YabaCollection> { collection in
    collection.collectionType == .folder
})
private var folders: [YabaCollection]
```

## 🤝 Contributing

### Code Style
- Follow Apple's Swift API Design Guidelines
- Use SwiftLint for code formatting
- Write comprehensive documentation

### Pull Request Process
1. Create feature branch
2. Update documentation
3. Submit pull request with detailed description

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

**Darwin Platform** - Native Apple experience with modern Swift technologies and complete privacy. 🍎✨ 