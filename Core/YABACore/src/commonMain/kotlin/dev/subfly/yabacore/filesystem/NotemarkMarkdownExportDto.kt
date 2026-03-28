package dev.subfly.yabacore.filesystem

import kotlinx.serialization.Serializable

@Serializable
data class NotemarkMarkdownExportAssetDto(
    val relativePath: String,
    val dataBase64: String,
)

@Serializable
data class NotemarkMarkdownExportBundleDto(
    val markdown: String,
    val assets: List<NotemarkMarkdownExportAssetDto> = emptyList(),
)
