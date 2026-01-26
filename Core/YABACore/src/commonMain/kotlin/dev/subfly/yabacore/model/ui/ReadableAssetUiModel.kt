package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.ReadableAssetRole

/**
 * A readable content asset (image) with absolute path.
 */
@Stable
data class ReadableAssetUiModel(
    val assetId: String,
    val role: ReadableAssetRole,
    /** Absolute path to the asset file */
    val absolutePath: String?,
)
