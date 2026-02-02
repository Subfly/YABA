package dev.subfly.yaba.core.components.markdown.util

import dev.subfly.yabacore.markdown.core.Range
import dev.subfly.yabacore.markdown.core.SectionKey
import dev.subfly.yabacore.model.ui.HighlightUiModel

/**
 * Returns (startInBlock, endInBlock) for a highlight that touches this block, or null if it doesn't.
 */
internal fun highlightRangeInBlock(
    h: HighlightUiModel,
    blockSectionKey: String,
    blockRange: Range,
): Pair<Int, Int>? {
    val blockIndex = SectionKey.parseBlockIndex(blockSectionKey) ?: return null
    val startBlockIndex = SectionKey.parseBlockIndex(h.startSectionKey) ?: return null
    val endBlockIndex = SectionKey.parseBlockIndex(h.endSectionKey) ?: return null
    if (blockIndex !in startBlockIndex..endBlockIndex) return null
    val startInBlock = if (blockIndex == startBlockIndex) h.startOffsetInSection else 0
    val endInBlock = if (blockIndex == endBlockIndex) h.endOffsetInSection else blockRange.length
    return startInBlock to endInBlock
}
