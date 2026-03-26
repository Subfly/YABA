@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.navigation.creation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.subfly.yabacore.icons.IconSubcategory
import dev.subfly.yabacore.model.annotation.ReadableSelectionDraft
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
            subclass(AnnotationCreationRoute::class, AnnotationCreationRoute.serializer())
            subclass(NotemarkTableCreationRoute::class, NotemarkTableCreationRoute.serializer())
            subclass(NotemarkMathSheetRoute::class, NotemarkMathSheetRoute.serializer())
            subclass(NotemarkLinkSheetRoute::class, NotemarkLinkSheetRoute.serializer())
            subclass(NotemarkLinkActionSheetRoute::class, NotemarkLinkActionSheetRoute.serializer())
            subclass(NotemarkMentionSheetRoute::class, NotemarkMentionSheetRoute.serializer())
            subclass(NotemarkMentionActionSheetRoute::class, NotemarkMentionActionSheetRoute.serializer())
            subclass(BookmarkSelectionRoute::class, BookmarkSelectionRoute.serializer())
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
    val initialUrl: String? = null,
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
    val allowTransparent: Boolean = true,
): NavKey

@Serializable
data class AnnotationCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String,
    val selectionDraft: ReadableSelectionDraft? = null,
    val annotationId: String? = null,
): NavKey

@Serializable
data class NotemarkTableCreationRoute(
    val routeId: String = Uuid.generateV4().toString(),
) : NavKey

@Serializable
data class NotemarkMathSheetRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val isBlock: Boolean,
    val initialLatex: String = "",
    val isEdit: Boolean = false,
    val editPos: Int? = null,
) : NavKey

@Serializable
data class NotemarkLinkSheetRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val initialText: String = "",
    val initialUrl: String = "",
    val isEdit: Boolean = false,
    val editPos: Int? = null,
) : NavKey

@Serializable
data class NotemarkLinkActionSheetRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val text: String,
    val url: String,
    val editPos: Int,
) : NavKey

@Serializable
data class NotemarkMentionSheetRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val initialText: String = "",
    val initialBookmarkId: String? = null,
    val isEdit: Boolean = false,
    val editPos: Int? = null,
) : NavKey

@Serializable
data class NotemarkMentionActionSheetRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val text: String,
    val bookmarkId: String,
    val bookmarkKindCode: Int,
    val editPos: Int,
) : NavKey

@Serializable
data class BookmarkSelectionRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val selectedBookmarkId: String? = null,
) : NavKey

@Serializable
data object EmptyCretionRoute: NavKey
