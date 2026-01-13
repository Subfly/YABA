package dev.subfly.yaba.core.navigation.main

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val detailNavigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeRoute::class, HomeRoute.serializer())
            subclass(SearchRoute::class, SearchRoute.serializer())
        }
    }
}

@Serializable
data object HomeRoute: NavKey

@Serializable
data object SearchRoute: NavKey
