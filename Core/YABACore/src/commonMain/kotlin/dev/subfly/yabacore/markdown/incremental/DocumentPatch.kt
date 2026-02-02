package dev.subfly.yabacore.markdown.incremental

import dev.subfly.yabacore.markdown.ast.BlockNode
import dev.subfly.yabacore.markdown.core.Range

/**
 * Output of incremental reparse: which blocks were added/removed/updated and the dirty range.
 */
data class DocumentPatch(
    val addedBlocks: List<BlockNode>,
    val removedBlockIds: List<String>,
    val updatedBlocks: List<BlockNode>,
    val dirtyRange: Range,
)
