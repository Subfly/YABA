@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.subfly.yabacore.icons.IconSubcategory
import dev.subfly.yabacore.model.utils.YabaColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val creationNavigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TagCreationRoute::class, TagCreationRoute.serializer())
            subclass(FolderCreationRoute::class, FolderCreationRoute.serializer())
            subclass(BookmarkCreationRoute::class, BookmarkCreationRoute.serializer())
            subclass(FolderSelectionRoute::class, FolderSelectionRoute.serializer())
            subclass(IconCategorySelectionRoute::class, IconCategorySelectionRoute.serializer())
            subclass(IconSelectionRoute::class, IconSelectionRoute.serializer())
            subclass(ColorSelectionRoute::class, ColorSelectionRoute.serializer())
        }
    }
}

@Serializable
data class TagCreationRoute(val tagId: String?): NavKey

@Serializable
data class FolderCreationRoute(val folderId: String?): NavKey

@Serializable
data class BookmarkCreationRoute(val bookmarkId: String?): NavKey

@Serializable
data object FolderSelectionRoute: NavKey

@Serializable
data class IconCategorySelectionRoute(val selectedIcon: String): NavKey

@Serializable
data class IconSelectionRoute(
    val selectedIcon: String,
    val selectedSubcategory: IconSubcategory,
): NavKey

@Serializable
data class ColorSelectionRoute(val color: YabaColor): NavKey
