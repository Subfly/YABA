package dev.subfly.yabacore.icons

import dev.subfly.yabacore.database.preload.readResourceText
import kotlinx.serialization.json.Json

object IconCatalog {
    private const val HEADER_PATH = "files/metadata/icon_categories_header.json"
    private const val METADATA_DIR = "files/metadata"

    private val json = Json { ignoreUnknownKeys = true }

    private val header by lazy {
        val raw = readResourceText(HEADER_PATH)
        json.decodeFromString<IconHeaderFile>(raw)
    }

    private val subcategoryCache = mutableMapOf<String, IconSubcategoryFile>()

    fun categories(): List<IconCategory> = header.categories

    fun subcategory(subcategoryId: String): IconSubcategory? =
        header.categories.flatMap { it.subcategories }.firstOrNull { it.id == subcategoryId }

    fun iconsForSubcategory(subcategoryId: String): List<IconItem> {
        val target = subcategory(subcategoryId) ?: return emptyList()
        val cached = subcategoryCache[subcategoryId]
        if (cached != null) return cached.icons

        val path = "$METADATA_DIR/${target.filename}"
        val raw = readResourceText(path)
        val parsed = json.decodeFromString<IconSubcategoryFile>(raw)
        subcategoryCache[subcategoryId] = parsed
        return parsed.icons
    }
}
