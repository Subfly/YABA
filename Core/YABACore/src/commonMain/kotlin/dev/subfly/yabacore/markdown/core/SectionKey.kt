package dev.subfly.yabacore.markdown.core

/**
 * Deterministic identifier for a block/section in the parsed document.
 * Used for highlight anchors and LazyColumn keys. Must be stable for the same
 * immutable document content (e.g. same contentVersion).
 *
 * For Linkmark readable content (immutable per contentVersion), use block index:
 * sectionKey = "b:<blockIndex>"
 */
object SectionKey {
    private const val PREFIX = "b:"

    fun fromBlockIndex(blockIndex: Int): String = "$PREFIX$blockIndex"

    fun parseBlockIndex(sectionKey: String): Int? {
        if (!sectionKey.startsWith(PREFIX)) return null
        return sectionKey.removePrefix(PREFIX).toIntOrNull()
    }
}
