# Compose Platform - YABA

The Android platform implementation of YABA using Compose Multiplatform for modern, native Android experiences with privacy and offline-first principles.

## 🤖 Platform Support

- **Android**: API 24+ (Android 7.0+)
- **Web**: Browser-based applications (planned)

## 📁 Project structure (YABA)

```
Compose/YABA/
├── composeApp/              # Compose Multiplatform app (Android)
│   ├── src/commonMain/      # Shared UI and logic
│   ├── src/androidMain/     # Android-specific code
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## 🏗️ Architecture

### Design Patterns
- **MVVM Architecture**: Model-View-ViewModel pattern with Compose
- **Repository Pattern**: Data access abstraction
- **Observer Pattern**: StateFlow for reactive state management
- **Dependency Injection**: Koin for dependency management
- **Clean Architecture**: Separation of concerns

### State Management
```kotlin
// ViewModel with StateFlow
class BookmarkViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BookmarkUiState())
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()
    
    fun createBookmark(url: String, label: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val bookmark = bookmarkRepository.createBookmark(url, label)
                _uiState.value = _uiState.value.copy(
                    bookmarks = _uiState.value.bookmarks + bookmark,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}
```

### Data Flow
```
User Action → Compose UI → ViewModel → UseCase → Repository → DataSource → Database/Network
```

## 🛠️ Technology Stack

### Core Technologies
- **Kotlin**: Primary programming language
- **Compose Multiplatform**: Cross-platform UI framework
- **Kotlin Coroutines**: Asynchronous programming
- **Kotlin Flow**: Reactive streams
- **Kotlinx Serialization**: JSON serialization

### Data & Persistence
- **SQLDelight**: Type-safe SQL database
- **Room**: Android database (alternative)
- **DataStore**: Preferences storage
- **Ktor Client**: HTTP networking

### Architecture & DI
- **Koin**: Dependency injection
- **ViewModel**: Architecture components
- **Navigation Compose**: Navigation framework
- **Hilt**: Alternative DI framework

### Build & Development
- **Gradle**: Build system
- **Android Studio**: IDE
- **Kotlin Multiplatform**: Cross-platform development

## 📱 Features

### Core Functionality
- **Bookmark Management**: Create, edit, delete, and organize bookmarks
- **Collections**: Folders and tags for organization
- **Search**: Full-text search with filters
- **Import/Export**: JSON, CSV, and HTML format support
- **Offline First**: All functionality works without internet connection

### Platform-Specific Features

#### Android
- Material Design 3 implementation
- Adaptive UI for different screen sizes
- Share intent integration
- Widget support
- Shortcuts integration
- Background sync

#### Web (Future)
- Progressive Web App (PWA)
- Service worker for offline support
- Web Share API integration
- Browser extension support

### Advanced Features
- **Link Previews**: Automatic metadata extraction
- **Reader Mode**: Distraction-free reading
- **Reminders**: Local notification reminders
- **Deep Linking**: Custom URL scheme support
- **Privacy Focused**: No data collection, no tracking, no analytics
- **Local Backup**: Local backup options

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog+
- JDK 17+
- Android SDK 34+
- Kotlin 1.9.0+

### Development Setup

1. **Clone and open the project**:
   ```bash
   cd Compose
   ./gradlew build
   ```

2. **Open in Android Studio**:
   - Open the `Compose` directory in Android Studio
   - Sync Gradle files
   - Select target device/emulator

3. **Run the application**:
   ```bash
   # Android (from `Compose/YABA`)
   ./gradlew :composeApp:installDebug
   ```

### Configuration

#### Android Setup
1. Configure signing in `androidApp/build.gradle.kts`
2. Configure deep linking in `AndroidManifest.xml`

## 📦 Building & Distribution

### Android Build
```bash
# Debug build
./gradlew androidApp:assembleDebug

# Release build
./gradlew androidApp:assembleRelease

# Bundle for Play Store
./gradlew androidApp:bundleRelease
```

### Web Build (Future)
```bash
# Build web app
./gradlew webApp:build

# Serve locally
./gradlew webApp:run
```

## 🔧 Configuration

### Environment Variables
- `DATABASE_URL`: Database connection string
- `API_BASE_URL`: Backend API URL

### Build Configuration
- **Debug**: Development features, verbose logging
- **Release**: Production optimizations, ProGuard/R8
- **Staging**: Testing configuration

### Platform-Specific Config
- **Android app**: `YABA/composeApp/build.gradle.kts`

## 📚 Code Examples

### Compose UI
```kotlin
@Composable
fun BookmarkScreen(
    viewModel: BookmarkViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column {
        LazyColumn {
            items(uiState.bookmarks) { bookmark ->
                BookmarkItem(
                    bookmark = bookmark,
                    onBookmarkClick = { viewModel.selectBookmark(it) }
                )
            }
        }
        
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
    }
}
```

### ViewModel
```kotlin
class BookmarkViewModel(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BookmarkUiState())
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()
    
    fun loadBookmarks() {
        viewModelScope.launch {
            bookmarkRepository.getBookmarks()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
                .collect { bookmarks ->
                    _uiState.value = _uiState.value.copy(bookmarks = bookmarks)
                }
        }
    }
}
```

### Repository
```kotlin
class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao,
    private val apiService: ApiService
) : BookmarkRepository {
    
    override fun getBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks()
    }
    
    override suspend fun createBookmark(url: String, label: String): Bookmark {
        val bookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            url = url,
            label = label,
            createdAt = System.currentTimeMillis()
        )
        
        bookmarkDao.insertBookmark(bookmark)
        return bookmark
    }
}
```

## 🤝 Contributing

### Code Style
- Follow Kotlin coding conventions
- Use ktlint for code formatting
- Write comprehensive documentation
- Follow Compose best practices

### Pull Request Process
1. Create feature branch
2. Update documentation
3. Ensure cross-platform compatibility
4. Submit pull request with detailed description

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

**Compose Platform** - Modern Android experiences with Kotlin Multiplatform and complete privacy. 🤖✨ 