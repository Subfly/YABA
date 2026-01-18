package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.common.IdGenerator

internal class IdResolver(
    existingIds: Set<String> = emptySet(),
) {
    private val used = existingIds.toMutableSet()
    private val mapping = mutableMapOf<String, String>()

    fun resolve(raw: String?): String {
        val key = raw?.takeIf { it.isNotBlank() }
        if (key != null && mapping.containsKey(key)) {
            return mapping.getValue(key)
        }

        // Use raw as-is if it looks like a valid UUID, otherwise generate new
        val candidate = if (key != null && isValidUuidFormat(key)) key else null
        val unique = generateUnique(candidate)

        if (key != null) {
            mapping[key] = unique
        }
        return unique
    }

    private fun generateUnique(candidate: String?): String {
        var value = candidate ?: IdGenerator.newId()
        while (used.contains(value)) {
            value = IdGenerator.newId()
        }
        used += value
        return value
    }

    private fun isValidUuidFormat(s: String): Boolean {
        // Simple check: UUID format is 8-4-4-4-12 hex chars
        val pattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return pattern.matches(s)
    }
}
