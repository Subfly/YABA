@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.preload

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.mappers.toFolderDomain
import dev.subfly.yabacore.database.mappers.toTagDomain
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.database.preload.model.PreloadData
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

/**
 * Seeds the database with bundled preload data (folders + tags) while emitting operation log
 * entries for sync.
 *
 * Ordering strategy:
 * - Sort preload items alphabetically by label (case-insensitive).
 * - Apply orders starting after the current max order in the table to avoid touching any
 * user-created ordering.
 */
object PreloadDataGenerator {
    private const val RESOURCE_NAME = "files/metadata/preload_data.json"
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.System
    private val folderDao
        get() = DatabaseProvider.folderDao
    private val tagDao
        get() = DatabaseProvider.tagDao
    private val opApplier
        get() = OpApplier

    suspend fun seedDefaultData(rawJsonOverride: String? = null): Boolean {
        val payload = rawJsonOverride ?: readResourceText(RESOURCE_NAME)
        val preloadData = decode(payload)
        val now = clock.now()

        val existingRootFolders = folderDao.getRoot()
        val existingFolderIds = existingRootFolders.map { it.id }.toSet()
        val folderOrderStart = (existingRootFolders.maxOfOrNull { it.order } ?: -1) + 1
        val foldersToInsert: List<FolderDomainModel> =
            preloadData
                .folders
                .map { it.toFolderDomain(now) }
                .filterNot { existingFolderIds.contains(it.id.toString()) }
                .sortedBy { it.label.lowercase() }
                .mapIndexed { index, entity ->
                    entity.copy(order = folderOrderStart + index)
                }

        val existingTags = tagDao.getAll()
        val existingTagIds = existingTags.map { it.id }.toSet()
        val tagOrderStart = (existingTags.maxOfOrNull { it.order } ?: -1) + 1
        val tagsToInsert: List<TagDomainModel> =
            preloadData
                .tags
                .map { it.toTagDomain(now) }
                .filterNot { existingTagIds.contains(it.id.toString()) }
                .sortedBy { it.label.lowercase() }
                .mapIndexed { index, entity -> entity.copy(order = tagOrderStart + index) }

        val drafts = buildList {
            foldersToInsert.forEach { add(it.toOperationDraft(OperationKind.CREATE)) }
            tagsToInsert.forEach { add(it.toOperationDraft(OperationKind.CREATE)) }
        }
        opApplier.applyLocal(drafts)

        return drafts.isNotEmpty()
    }

    private fun decode(content: String): PreloadData =
        json.decodeFromString(PreloadData.serializer(), content)
}
