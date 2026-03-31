package dev.subfly.yaba.core.model.ui

import androidx.compose.runtime.Stable

/**
 * A readable content asset (image) with absolute path.
 */
@Stable
data class ReadableAssetUiModel(
    val assetId: String,
    /** Absolute path to the asset file */
    val absolutePath: String?,
)
