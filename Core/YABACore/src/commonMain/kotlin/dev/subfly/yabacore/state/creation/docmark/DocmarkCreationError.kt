package dev.subfly.yabacore.state.creation.docmark

import androidx.compose.runtime.Stable

@Stable
sealed class DocmarkCreationError {
    @Stable
    data object NoPdf : DocmarkCreationError()

    @Stable
    data object PdfReadFailed : DocmarkCreationError()

    @Stable
    data object SaveFailed : DocmarkCreationError()
}
