@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.impex.internal

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class IdResolver(
    existingIds: Set<Uuid> = emptySet(),
) {
    private val used = existingIds.toMutableSet()
    private val mapping = mutableMapOf<String, Uuid>()

    fun resolve(raw: String?): Uuid {
        val key = raw?.takeIf { it.isNotBlank() }
        if (key != null && mapping.containsKey(key)) {
            return mapping.getValue(key)
        }

        val parsed = key?.let { runCatching { Uuid.parse(it) }.getOrNull() }
        val unique = generateUnique(parsed)

        if (key != null) {
            mapping[key] = unique
        }
        return unique
    }

    private fun generateUnique(candidate: Uuid?): Uuid {
        var value = candidate ?: Uuid.random()
        while (used.contains(value)) {
            value = Uuid.random()
        }
        used += value
        return value
    }
}
