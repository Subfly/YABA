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
