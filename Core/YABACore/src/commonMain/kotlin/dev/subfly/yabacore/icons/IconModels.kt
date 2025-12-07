package dev.subfly.yabacore.icons

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IconHeaderMetadata(
    @SerialName("total_categories") val totalCategories: Int? = null,
    @SerialName("total_subcategories") val totalSubcategories: Int? = null,
    @SerialName("total_icons") val totalIcons: Int? = null,
    val version: String? = null,
    val description: String? = null,
)

@Serializable
internal data class IconHeaderFile(
    val metadata: IconHeaderMetadata,
    val categories: List<IconCategory>,
)

@Serializable
internal data class IconCategory(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("icon_count") val iconCount: Int,
    val filename: String,
    @SerialName("header_icon") val headerIcon: String,
    val color: Int,
    val subcategories: List<IconSubcategory>,
)

@Serializable
internal data class IconSubcategory(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("header_icon") val headerIcon: String,
    val color: Int,
    @SerialName("icon_count") val iconCount: Int,
    val filename: String,
)

@Serializable
internal data class IconSubcategoryMetadata(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("main_category") val mainCategory: String,
    @SerialName("icon_count") val iconCount: Int,
    val version: String? = null,
)

@Serializable
internal data class IconSubcategoryFile(
    val metadata: IconSubcategoryMetadata,
    val icons: List<IconItem>,
)

@Serializable
internal data class IconItem(
    val name: String,
    val tags: String,
    val category: String,
)
