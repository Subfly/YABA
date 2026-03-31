@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.navigation.main

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val detailNavigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeRoute::class, HomeRoute.serializer())
            subclass(SearchRoute::class, SearchRoute.serializer())
            subclass(FolderDetailRoute::class, FolderDetailRoute.serializer())
            subclass(TagDetailRoute::class, TagDetailRoute.serializer())
            subclass(LinkDetailRoute::class, LinkDetailRoute.serializer())
            subclass(NoteDetailRoute::class, NoteDetailRoute.serializer())
            subclass(ImageDetailRoute::class, ImageDetailRoute.serializer())
            subclass(DocDetailRoute::class, DocDetailRoute.serializer())
        }
    }
}

@Serializable
data class HomeRoute(
    val routeId: String = Uuid.generateV4().toString(),
): NavKey

@Serializable
data class SearchRoute(
    val routeId: String = Uuid.generateV4().toString(),
): NavKey

@Serializable
data class FolderDetailRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val folderId: String,
): NavKey

@Serializable
data class TagDetailRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val tagId: String,
): NavKey

@Serializable
data class LinkDetailRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String,
): NavKey

@Serializable
data class NoteDetailRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String,
): NavKey

@Serializable
data class ImageDetailRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String,
): NavKey

@Serializable
data class DocDetailRoute(
    val routeId: String = Uuid.generateV4().toString(),
    val bookmarkId: String,
): NavKey
