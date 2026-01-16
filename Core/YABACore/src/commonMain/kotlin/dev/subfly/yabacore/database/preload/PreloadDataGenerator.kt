@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.preload

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.mappers.toFolderDomain
import dev.subfly.yabacore.database.mappers.toTagDomain
import dev.subfly.yabacore.database.preload.model.PreloadData
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

/**
 * Seeds the database with bundled preload data (folders + tags) using filesystem-first approach.
 *
 * Ordering strategy:
 * - Sort preload items alphabetically by label (case-insensitive).
 * - Apply orders starting after the current max order in the table to avoid touching any
 *   user-created ordering.
 */
object PreloadDataGenerator {
    private const val RESOURCE_NAME = "files/metadata/preload_data.json"
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.System
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val entityFileManager get() = EntityFileManager

    suspend fun seedDefaultData(rawJsonOverride: String? = null): Boolean {
        val payload = rawJsonOverride ?: readResourceText(RESOURCE_NAME)
        val preloadData = decode(payload)
        val now = clock.now()
        val deviceId = DeviceIdProvider.get()

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

        var inserted = false

        // Insert folders using filesystem-first approach
        foldersToInsert.forEach { folder ->
            val initialClock = VectorClock.of(deviceId, 1)
            val folderJson = FolderMetaJson(
                id = folder.id.toString(),
                parentId = folder.parentId?.toString(),
                label = folder.label,
                description = folder.description,
                icon = folder.icon,
                colorCode = folder.color.code,
                order = folder.order,
                createdAt = folder.createdAt.toEpochMilliseconds(),
                editedAt = folder.editedAt.toEpochMilliseconds(),
                clock = initialClock.toMap(),
            )

            // 1. Write to filesystem
            entityFileManager.writeFolderMeta(folderJson)

            // 2. Update SQLite cache
            folderDao.upsert(
                FolderEntity(
                    id = folder.id.toString(),
                    parentId = folder.parentId?.toString(),
                    label = folder.label,
                    description = folder.description,
                    icon = folder.icon,
                    color = folder.color,
                    order = folder.order,
                    createdAt = folder.createdAt.toEpochMilliseconds(),
                    editedAt = folder.editedAt.toEpochMilliseconds(),
                )
            )
            inserted = true
        }

        // Insert tags using filesystem-first approach
        tagsToInsert.forEach { tag ->
            val initialClock = VectorClock.of(deviceId, 1)
            val tagJson = TagMetaJson(
                id = tag.id.toString(),
                label = tag.label,
                icon = tag.icon,
                colorCode = tag.color.code,
                order = tag.order,
                createdAt = tag.createdAt.toEpochMilliseconds(),
                editedAt = tag.editedAt.toEpochMilliseconds(),
                clock = initialClock.toMap(),
            )

            // 1. Write to filesystem
            entityFileManager.writeTagMeta(tagJson)

            // 2. Update SQLite cache
            tagDao.upsert(
                TagEntity(
                    id = tag.id.toString(),
                    label = tag.label,
                    icon = tag.icon,
                    color = tag.color,
                    order = tag.order,
                    createdAt = tag.createdAt.toEpochMilliseconds(),
                    editedAt = tag.editedAt.toEpochMilliseconds(),
                )
            )
            inserted = true
        }

        return inserted
    }

    private fun decode(content: String): PreloadData =
        json.decodeFromString(PreloadData.serializer(), content)
}
