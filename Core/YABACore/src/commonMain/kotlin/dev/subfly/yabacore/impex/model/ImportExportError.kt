package dev.subfly.yabacore.impex.model

/**
 * Errors carry a message key so the UI can localize using its own string tables (mirrors
 * LocalizedStringKey usage on Darwin).
 */
sealed class ImportExportError(
    open val messageKey: String,
    open val args: Map<String, String?> = emptyMap(),
) : Exception(messageKey) {

    // JSON
    object InvalidJson :
        ImportExportError(
            messageKey = "Data Manager Invalid JSON Format Message",
        )

    // CSV
    object InvalidCsv :
        ImportExportError(
            messageKey = "Data Manager Invalid CSV Encoding Message",
        )

    object EmptyCsv :
        ImportExportError(
            messageKey = "Data Manager Empty CSV Message",
        )

    data class InvalidCsvHeader(val header: List<String>) :
        ImportExportError(
            messageKey = "Data Manager Invalid CSV Encoding Message",
            args = mapOf("header" to header.joinToString(",")),
        )

    data class MissingRequiredField(val fieldName: String) :
        ImportExportError(
            messageKey = "Data Manager Bookmark URL Column Not Selected Message",
            args = mapOf("field" to fieldName),
        )

    // HTML
    object EmptyHtml :
        ImportExportError(
            messageKey = "Data Manager Empty HTML Message",
        )

    data class InvalidBookmarkUrl(val url: String) :
        ImportExportError(
            messageKey = "Data Manager Invalid Bookmark URL Message",
            args = mapOf("url" to url),
        )

    data class Unknown(val reason: String) :
        ImportExportError(
            messageKey = "Data Manager Unknown Error Message",
            args = mapOf("reason" to reason),
        )
}
