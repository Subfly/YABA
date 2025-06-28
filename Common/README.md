# Common Libraries - YABA

Shared libraries and business logic for YABA using Kotlin Multiplatform, providing cross-platform functionality for both Darwin and Compose implementations with privacy and offline-first principles.

## 🌐 Cross-Platform Support

- **Darwin**: iOS, macOS, visionOS (via Kotlin/Native)
- **Compose**: Android, JVM, Web (via Kotlin Multiplatform)
- **Future**: Additional platforms as needed

## 📁 Planned Project Structure

```
Common/
├── src/
│   ├── commonMain/          # Shared Kotlin code
│   │   ├── kotlin/
│   │   │   ├── data/       # Data models and repositories
│   │   │   ├── domain/     # Business logic and use cases
│   │   │   ├── network/    # HTTP client and API services
│   │   │   ├── storage/    # Database and preferences
│   │   │   ├── sync/       # Synchronization logic
│   │   │   ├── utils/      # Utility functions
│   │   │   └── validation/ # Data validation
│   │   └── resources/      # Shared resources
│   ├── androidMain/         # Android-specific implementations
│   ├── iosMain/            # iOS-specific implementations
│   ├── desktopMain/        # Desktop-specific implementations
│   ├── webMain/            # Web-specific implementations (future)
│   └── commonTest/         # Shared test code
├── build.gradle.kts        # Build configuration
└── README.md              # This file
```

## 🏗️ Architecture

### Design Patterns
- **Clean Architecture**: Separation of concerns with layers
- **Repository Pattern**: Data access abstraction
- **Use Case Pattern**: Business logic encapsulation
- **Dependency Injection**: Platform-agnostic DI
- **Observer Pattern**: Reactive data streams

### Layer Structure
```
Presentation Layer (Platform-specific)
    ↓
Domain Layer (Common)
    ↓
Data Layer (Common with platform-specific implementations)
    ↓
Platform Layer (Platform-specific)
```

### State Management
```kotlin
// Shared state management
class SharedBookmarkState {
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()
    
    fun updateBookmarks(newBookmarks: List<Bookmark>) {
        _bookmarks.value = newBookmarks
    }
}
```

## 🛠️ Technology Stack

### Core Technologies
- **Kotlin Multiplatform**: Cross-platform development
- **Kotlin Coroutines**: Asynchronous programming
- **Kotlin Flow**: Reactive streams
- **Kotlinx Serialization**: JSON serialization
- **Kotlinx DateTime**: Date and time handling

### Data & Persistence
- **SQLDelight**: Type-safe SQL database
- **DataStore**: Preferences storage
- **Ktor Client**: HTTP networking
- **Kotlinx Serialization**: Data serialization

### Architecture & DI
- **Koin**: Dependency injection
- **Kotlinx Coroutines**: Concurrency
- **Result**: Error handling
- **Arrow**: Functional programming utilities

## 📦 Modules

### Data Models
```kotlin
@Serializable
data class Bookmark(
    val id: String,
    val url: String,
    val label: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val collections: List<String> = emptyList(),
    val metadata: BookmarkMetadata? = null
)

@Serializable
data class Collection(
    val id: String,
    val name: String,
    val type: CollectionType,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

### Domain Layer
```kotlin
// Use cases for business logic
class CreateBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository,
    private val urlValidator: UrlValidator
) {
    suspend operator fun invoke(
        url: String,
        label: String,
        description: String = "",
        collections: List<String> = emptyList()
    ): Result<Bookmark> {
        return try {
            if (!urlValidator.isValid(url)) {
                return Result.failure(InvalidUrlException(url))
            }
            
            val bookmark = Bookmark(
                id = UUID.randomUUID().toString(),
                url = url,
                label = label,
                description = description,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                collections = collections
            )
            
            val savedBookmark = bookmarkRepository.createBookmark(bookmark)
            Result.success(savedBookmark)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Repository Layer
```kotlin
interface BookmarkRepository {
    fun getBookmarks(): Flow<List<Bookmark>>
    fun getBookmarkById(id: String): Flow<Bookmark?>
    suspend fun createBookmark(bookmark: Bookmark): Bookmark
    suspend fun updateBookmark(bookmark: Bookmark): Bookmark
    suspend fun deleteBookmark(id: String): Boolean
    suspend fun searchBookmarks(query: String): List<Bookmark>
}

interface CollectionRepository {
    fun getCollections(): Flow<List<Collection>>
    fun getCollectionById(id: String): Flow<Collection?>
    suspend fun createCollection(collection: Collection): Collection
    suspend fun updateCollection(collection: Collection): Collection
    suspend fun deleteCollection(id: String): Boolean
}
```

### Network Layer
```kotlin
class ApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    suspend fun syncBookmarks(bookmarks: List<Bookmark>): Result<List<Bookmark>> {
        return try {
            val response = httpClient.post("$baseUrl/bookmarks/sync") {
                setBody(bookmarks)
            }
            
            if (response.status.isSuccess()) {
                val syncedBookmarks = response.body<List<Bookmark>>()
                Result.success(syncedBookmarks)
            } else {
                Result.failure(ApiException("Sync failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 🚀 Getting Started

### Prerequisites
- Kotlin 1.9.0+
- Gradle 8.0+
- Platform-specific SDKs

### Development Setup

1. **Add to your project**:
   ```kotlin
   // In your platform's build.gradle.kts
   dependencies {
       implementation(project(":common"))
   }
   ```

2. **Configure the module**:
   ```kotlin
   // settings.gradle.kts
   include(":common")
   ```

3. **Build the library**:
   ```bash
   ./gradlew :common:build
   ```

### Platform Integration

#### Darwin Integration
```swift
// Import the Kotlin library
import Common

// Use shared functionality
let bookmark = Bookmark(
    id: "123",
    url: "https://example.com",
    label: "Example",
    createdAt: Date().timeIntervalSince1970 * 1000
)
```

#### Compose Integration
```kotlin
// Import the shared module
import com.yaba.common.domain.CreateBookmarkUseCase

// Use shared functionality
val useCase = CreateBookmarkUseCase(bookmarkRepository, urlValidator)
val result = useCase("https://example.com", "Example")
```

## 📦 Building & Distribution

### Build Configuration
```kotlin
// build.gradle.kts
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("io.ktor:ktor-client-core:2.3.4")
                implementation("com.squareup.sqldelight:runtime:1.5.5")
            }
        }
        
        androidMain {
            dependencies {
                implementation("io.ktor:ktor-client-android:2.3.4")
                implementation("com.squareup.sqldelight:android-driver:1.5.5")
            }
        }
        
        iosMain {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.4")
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
            }
        }
    }
}
```

### Publishing
```bash
# Build for all platforms
./gradlew :common:build

# Publish to local repository
./gradlew :common:publishToMavenLocal

# Publish to remote repository
./gradlew :common:publish
```

## 🔧 Configuration

### Platform-Specific Configuration
- **Android**: `androidMain/AndroidManifest.xml`
- **iOS**: `iosMain/Info.plist`
- **Desktop**: `desktopMain/application.conf`

## 📚 Code Examples

### Data Validation
```kotlin
class UrlValidator {
    fun isValid(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }
    
    fun extractDomain(url: String): String? {
        return try {
            URL(url).host
        } catch (e: MalformedURLException) {
            null
        }
    }
}
```

### Error Handling
```kotlin
sealed class BookmarkError : Exception() {
    object InvalidUrl : BookmarkError()
    object NetworkError : BookmarkError()
    object DatabaseError : BookmarkError()
    data class ValidationError(val field: String, val message: String) : BookmarkError()
}

fun handleBookmarkError(error: BookmarkError): String {
    return when (error) {
        is BookmarkError.InvalidUrl -> "Invalid URL provided"
        is BookmarkError.NetworkError -> "Network connection failed"
        is BookmarkError.DatabaseError -> "Database operation failed"
        is BookmarkError.ValidationError -> "Validation failed: ${error.message}"
    }
}
```

### Synchronization
```kotlin
class SyncManager(
    private val localRepository: BookmarkRepository,
    private val remoteRepository: BookmarkRepository,
    private val syncScheduler: SyncScheduler
) {
    suspend fun syncBookmarks(): Result<SyncResult> {
        return try {
            val localBookmarks = localRepository.getBookmarks().first()
            val remoteBookmarks = remoteRepository.getBookmarks().first()
            
            val mergedBookmarks = mergeBookmarks(localBookmarks, remoteBookmarks)
            
            localRepository.updateBookmarks(mergedBookmarks)
            remoteRepository.updateBookmarks(mergedBookmarks)
            
            Result.success(SyncResult.Success(mergedBookmarks.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 🤝 Contributing

### Code Style
- Follow Kotlin coding conventions
- Use ktlint for code formatting
- Write comprehensive documentation
- Ensure cross-platform compatibility

### Pull Request Process
1. Create feature branch
2. Update documentation
3. Ensure cross-platform compatibility
4. Submit pull request with detailed description

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

**Common Libraries** - Shared functionality for cross-platform bookmark management with complete privacy. 🌐✨ 