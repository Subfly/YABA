package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.unfurl.ReadableHeadingLevel
import dev.subfly.yabacore.unfurl.ReadableInline

@Immutable
data class ReadableDocumentUiModel(
    val title: String?,
    val author: String?,
    val blocks: List<ReadableBlockUiModel>,
)

@Immutable
sealed interface ReadableBlockUiModel {
    val id: String

    @Immutable
    data class Paragraph(
        override val id: String,
        val inlines: List<ReadableInline>,
    ) : ReadableBlockUiModel

    @Immutable
    data class Heading(
        override val id: String,
        val level: ReadableHeadingLevel,
        val inlines: List<ReadableInline>,
    ) : ReadableBlockUiModel

    @Immutable
    data class Image(
        override val id: String,
        val assetId: String,
        val assetPath: String?,
        val role: ReadableAssetRole,
        val caption: String? = null,
    ) : ReadableBlockUiModel

    @Immutable
    data class Code(
        override val id: String,
        val language: String? = null,
        val text: String,
    ) : ReadableBlockUiModel

    @Immutable
    data class ListBlock(
        override val id: String,
        val ordered: Boolean,
        val items: List<ReadableListItemUiModel>,
    ) : ReadableBlockUiModel

    @Immutable
    data class Quote(
        override val id: String,
        val children: List<ReadableBlockUiModel>,
    ) : ReadableBlockUiModel

    @Immutable
    data class Divider(
        override val id: String,
    ) : ReadableBlockUiModel
}

@Immutable
data class ReadableListItemUiModel(
    val blocks: List<ReadableBlockUiModel>,
)
