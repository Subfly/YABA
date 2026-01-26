package dev.subfly.yabacore.model.utils

enum class ReadableAssetRole {
    INLINE,
    HERO,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): ReadableAssetRole {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when (normalized) {
                "hero" -> HERO
                "inline" -> INLINE
                else -> UNKNOWN
            }
        }
    }
}
