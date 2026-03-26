package dev.subfly.yabacore.state.creation.docmark

import androidx.compose.runtime.Stable

@Stable
sealed class DocmarkCreationError {
    @Stable
    data object NoDocument : DocmarkCreationError()

    @Stable
    data object DocumentReadFailed : DocmarkCreationError()

    @Stable
    data object PreviewExtractionFailed : DocmarkCreationError()

    @Stable
    data object SaveFailed : DocmarkCreationError()
}
