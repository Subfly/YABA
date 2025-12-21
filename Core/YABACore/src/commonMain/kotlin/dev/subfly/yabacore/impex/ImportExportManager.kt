@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.impex

import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.impex.internal.CsvImporter
import dev.subfly.yabacore.impex.internal.CsvUtils
import dev.subfly.yabacore.impex.internal.Exporters
import dev.subfly.yabacore.impex.internal.ExistingIds
import dev.subfly.yabacore.impex.internal.HtmlImporter
import dev.subfly.yabacore.impex.internal.ImportBundle
import dev.subfly.yabacore.impex.internal.ImportExportDataSource
import dev.subfly.yabacore.impex.internal.JsonImporter
import dev.subfly.yabacore.impex.internal.toOperationDrafts
import dev.subfly.yabacore.impex.model.ExportFormat
import dev.subfly.yabacore.impex.model.ImportSummary
import dev.subfly.yabacore.impex.model.MappableCsvHeader
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Facade for import/export operations implemented in the shared Kotlin core.
 *
 * The API mirrors the old Swift DataManager entry-points so the Darwin code can migrate
 * without adopting DI or MVVM patterns.
 */
object ImportExportManager {
    suspend fun importJson(bytes: ByteArray): ImportSummary =
        import { existing ->
            JsonImporter.buildBundle(
                JsonImporter.decodeContent(bytes),
                existing
            )
        }

    suspend fun importCsv(bytes: ByteArray): ImportSummary =
        import { existing -> CsvImporter.import(bytes, existing) }

    suspend fun importMappedCsv(
        bytes: ByteArray,
        headers: Map<MappableCsvHeader, Int?>,
    ): ImportSummary = import { existing ->
        CsvImporter.importMapped(bytes, headers, existing)
    }

    suspend fun importHtml(bytes: ByteArray): ImportSummary =
        import { existing -> HtmlImporter.import(bytes, existing) }

    suspend fun export(format: ExportFormat): ByteArray =
        withContext(Dispatchers.Default) {
            val snapshot = ImportExportDataSource.loadSnapshot()
            Exporters.export(snapshot, format)
        }

    fun extractCsvHeaders(bytes: ByteArray): List<String> {
        val content = bytes.decodeToString()
        val firstLine = content
            .lineSequence()
            .firstOrNull { it.isNotBlank() } ?: return emptyList()
        return CsvUtils.parseRow(firstLine)
    }

    private suspend fun import(
        builder: suspend (existing: ExistingIds) -> ImportBundle,
    ): ImportSummary = withContext(Dispatchers.Default) {
        val existing = ImportExportDataSource.existingIds()
        val bundle = builder(existing)
        OpApplier.applyLocal(bundle.toOperationDrafts())
        ImportSummary(
            folders = bundle.folders.size,
            tags = bundle.tags.size,
            bookmarks = bundle.bookmarks.size,
        )
    }
}
