@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.navigation.creation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.subfly.yabacore.icons.IconSubcategory
import dev.subfly.yabacore.model.utils.FolderSelectionMode
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
            subclass(LinkmarkCreationRoute::class, LinkmarkCreationRoute.serializer())
            subclass(NotemarkCreationRoute::class, NotemarkCreationRoute.serializer())
            subclass(DocmarkCreationRoute::class, DocmarkCreationRoute.serializer())
            subclass(ImagemarkCreationRoute::class, ImagemarkCreationRoute.serializer())
            subclass(FolderSelectionRoute::class, FolderSelectionRoute.serializer())
            subclass(TagSelectionRoute::class, TagSelectionRoute.serializer())
            subclass(IconCategorySelectionRoute::class, IconCategorySelectionRoute.serializer())
            subclass(IconSelectionRoute::class, IconSelectionRoute.serializer())
            subclass(ColorSelectionRoute::class, ColorSelectionRoute.serializer())
            subclass(ImageSelectionRoute::class, ImageSelectionRoute.serializer())
            subclass(EmptyCretionRoute::class, EmptyCretionRoute.serializer())
        }
    }
}

@Serializable
data class TagCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val tagId: String?,
): NavKey

@Serializable
data class FolderCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val folderId: String?,
): NavKey

@Serializable
data class BookmarkCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
): NavKey

@Serializable
data class LinkmarkCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String?,
): NavKey

@Serializable
data class NotemarkCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String?,
): NavKey

@Serializable
data class ImagemarkCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String?,
): NavKey

@Serializable
data class DocmarkCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String?,
): NavKey

@Serializable
data class FolderSelectionRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val mode: FolderSelectionMode,
    val contextFolderId: String?,
    val contextBookmarkIds: List<String>?,
): NavKey

@Serializable
data class TagSelectionRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val selectedTagIds: List<String>,
): NavKey

@Serializable
data class IconCategorySelectionRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val selectedIcon: String,
): NavKey

@Serializable
data class IconSelectionRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val selectedIcon: String,
    val selectedSubcategory: IconSubcategory,
): NavKey

@Serializable
data class ImageSelectionRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val selectedImage: String?,
    val imageDataMap: Map<String, ByteArray>,
): NavKey

@Serializable
data class ColorSelectionRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val selectedColor: YabaColor,
): NavKey

@Serializable
data object EmptyCretionRoute: NavKey
