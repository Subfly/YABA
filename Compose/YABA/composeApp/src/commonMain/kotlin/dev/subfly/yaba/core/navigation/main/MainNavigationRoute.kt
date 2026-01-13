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
