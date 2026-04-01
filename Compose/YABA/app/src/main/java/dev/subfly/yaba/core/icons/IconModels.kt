package dev.subfly.yaba.core.icons

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IconHeaderMetadata(
    @SerialName("total_categories") val totalCategories: Int? = null,
    @SerialName("total_icons") val totalIcons: Int? = null,
    val version: String? = null,
)

@Serializable
data class IconHeaderFile(
    val metadata: IconHeaderMetadata,
    val categories: List<IconCategory>,
)

@Serializable
@Stable
data class IconCategory(
    val id: String,
    val name: String,
    @SerialName("icon_count") val iconCount: Int,
    val filename: String,
    @SerialName("header_icon") val headerIcon: String,
    val color: Int,
)

@Serializable
internal data class IconCategoryMetadata(
    val id: String,
    val name: String,
    @SerialName("main_category") val mainCategory: String,
    @SerialName("icon_count") val iconCount: Int,
    val version: String? = null,
)

@Serializable
internal data class IconCategoryFile(
    val metadata: IconCategoryMetadata,
    val icons: List<IconItem>,
)

@Serializable
@Stable
data class IconItem(
    val name: String,
)
