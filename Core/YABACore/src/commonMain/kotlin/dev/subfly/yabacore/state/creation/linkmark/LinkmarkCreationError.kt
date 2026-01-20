package dev.subfly.yabacore.state.creation.linkmark

/**
 * Error types for linkmark creation that can be localized on the UI side.
 */
sealed class LinkmarkCreationError {
    /** The URL could not be parsed or is invalid. */
    data class InvalidUrl(val rawUrl: String) : LinkmarkCreationError()

    /** Unable to fetch link preview data. */
    data object UnableToUnfurl : LinkmarkCreationError()

    /** General fetch failure (network, timeout, etc.). */
    data object FetchFailed : LinkmarkCreationError()

    /** Failed to save the bookmark. */
    data object SaveFailed : LinkmarkCreationError()
}

