@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
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
            subclass(EmptyRoute::class, EmptyRoute.serializer())
        }
    }
}

@Serializable
data class TagCreationRoute(val tagId: Uuid?): NavKey

@Serializable
data class FolderCreationRoute(val folderId: Uuid?): NavKey

@Serializable
data class BookmarkCreationRoute(val bookmarkId: Uuid?): NavKey

@Serializable
data object FolderSelectionRoute: NavKey

@Serializable
data object IconCategorySelectionRoute: NavKey

@Serializable
data object IconSelectionRoute: NavKey

@Serializable
data class ColorSelectionRoute(val color: YabaColor): NavKey

@Serializable
data object EmptyRoute: NavKey