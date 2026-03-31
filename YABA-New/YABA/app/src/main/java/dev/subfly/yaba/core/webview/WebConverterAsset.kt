package dev.subfly.yaba.core.webview

import androidx.compose.runtime.Stable

/**
 * Asset placeholder from converter output (e.g. yaba-asset://N → remote URL).
 */
@Stable
data class WebConverterAsset(
    val placeholder: String,
    val url: String,
    val alt: String? = null,
)
