package dev.subfly.yaba.core.state.creation.imagemark

import androidx.compose.runtime.Stable

/**
 * Error types for imagemark creation that can be localized on the UI side.
 */
@Stable
sealed class ImagemarkCreationError {
    /** No image selected. */
    @Stable
    data object NoImage : ImagemarkCreationError()

    /** Failed to read image from selected file. */
    @Stable
    data object ImageReadFailed : ImagemarkCreationError()

    /** Failed to save the bookmark. */
    @Stable
    data object SaveFailed : ImagemarkCreationError()
}
