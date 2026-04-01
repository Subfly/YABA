package dev.subfly.yaba.core.managers

import dev.subfly.yaba.core.icons.IconCategory
import dev.subfly.yaba.core.icons.IconHeaderFile
import dev.subfly.yaba.core.icons.IconItem
import dev.subfly.yaba.core.icons.IconSubcategory
import dev.subfly.yaba.core.icons.IconSubcategoryFile
import dev.subfly.yaba.core.util.BundleReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Read-only access to bundled icon taxonomy and per-subcategory icon lists.
 *
 * Requires [dev.subfly.yaba.core.filesystem.access.FileAccessProvider.initialize] before first use
 * (bundled JSON is read from `assets/files/metadata/`).
 *
 * Does not hold UI state or flows; callers own loading and caching in state machines / VMs.
 */
object IconManager {
    private const val HEADER_PATH = "files/metadata/icon_categories_header.json"
    private const val METADATA_DIR = "files/metadata"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Loads the full category tree (each category includes its subcategories and metadata).
     */
    suspend fun loadAllCategories(): List<IconCategory> =
        withContext(Dispatchers.IO) {
            val raw = BundleReader.readAssetText(HEADER_PATH)
            val parsed = json.decodeFromString<IconHeaderFile>(raw)
            parsed.categories
        }

    /**
     * Loads icon names for the given [subcategory] from its bundled JSON file.
     */
    suspend fun loadIconsForSubcategory(subcategory: IconSubcategory): List<IconItem> =
        withContext(Dispatchers.IO) {
            val path = "$METADATA_DIR/${subcategory.filename}"
            val raw = BundleReader.readAssetText(path)
            val parsed = json.decodeFromString<IconSubcategoryFile>(raw)
            parsed.icons
        }
}
