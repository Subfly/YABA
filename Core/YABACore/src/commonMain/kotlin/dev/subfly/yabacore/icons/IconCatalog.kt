package dev.subfly.yabacore.icons

import dev.subfly.yabacore.database.preload.readResourceText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Icon catalog that exposes two flows:
 * - [categoriesFlow]: List of icon categories (loaded on init)
 * - [iconsFlow]: List of icons for the currently selected subcategory
 *
 * Call [loadIcons] with a subcategory ID to populate [iconsFlow].
 */
object IconCatalog {
    private const val HEADER_PATH = "files/metadata/icon_categories_header.json"
    private const val METADATA_DIR = "files/metadata"

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache for already-loaded subcategory files
    private val subcategoryCache = mutableMapOf<String, IconSubcategoryFile>()

    // Header data (categories + subcategory metadata)
    private var header: IconHeaderFile? = null

    private val _categories = MutableStateFlow<List<IconCategory>>(emptyList())
    val categoriesFlow: StateFlow<List<IconCategory>> = _categories.asStateFlow()

    private val _icons = MutableStateFlow<List<IconItem>>(emptyList())
    val iconsFlow: StateFlow<List<IconItem>> = _icons.asStateFlow()

    init {
        loadHeader()
    }

    /** Load icons for the given [subcategoryId]. Updates [iconsFlow] when complete. */
    fun loadIcons(subcategoryId: String) {
        // Clear current icons immediately (UI will show loading)
        _icons.update { emptyList() }

        scope.launch {
            // Check cache first
            val cached = subcategoryCache[subcategoryId]
            if (cached != null) {
                _icons.update { cached.icons }
                return@launch
            }

            // Find the subcategory in the header to get its filename
            val target = header?.categories?.flatMap { it.subcategories }?.firstOrNull {
                it.id == subcategoryId
            } ?: return@launch

            val path = "$METADATA_DIR/${target.filename}"
            val raw = readResourceText(path)
            val parsed = json.decodeFromString<IconSubcategoryFile>(raw)
            subcategoryCache[subcategoryId] = parsed
            _icons.update { parsed.icons }
        }
    }

    /** Clear current icons (e.g., when navigating back to categories). */
    fun resetIcons() {
        _icons.update { emptyList() }
    }

    private fun loadHeader() {
        scope.launch {
            val raw = readResourceText(HEADER_PATH)
            val parsed = json.decodeFromString<IconHeaderFile>(raw)
            header = parsed
            _categories.update { parsed.categories }
        }
    }
}
