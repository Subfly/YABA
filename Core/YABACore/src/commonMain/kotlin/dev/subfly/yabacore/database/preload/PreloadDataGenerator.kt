@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.preload

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toFolderEntity
import dev.subfly.yabacore.database.mappers.toTagEntity
import dev.subfly.yabacore.database.preload.model.PreloadData
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

/**
 * Seeds the database with bundled preload data (folders + tags).
 */
object PreloadDataGenerator {
    private const val RESOURCE_NAME = "files/metadata/preload_data.json"
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.System
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao

    suspend fun seedDefaultData(rawJsonOverride: String? = null): Boolean {
        val payload = rawJsonOverride ?: readResourceText(RESOURCE_NAME)
        val preloadData = decode(payload)
        val now = clock.now()

        val existingRootFolders = folderDao.getFoldersByParent(parentId = null, includeHidden = true)
        val existingFolderIds = existingRootFolders.map { it.id }.toSet()
        val foldersToInsert = preloadData.folders
            .map { it.toFolderEntity(now) }
            .filterNot { existingFolderIds.contains(it.id) }
            .sortedBy { it.label.lowercase() }

        val existingTags = tagDao.getAll()
        val existingTagIds = existingTags.map { it.id }.toSet()
        val tagsToInsert = preloadData.tags
            .map { it.toTagEntity(now) }
            .filterNot { existingTagIds.contains(it.id) }
            .sortedBy { it.label.lowercase() }

        var inserted = false
        foldersToInsert.forEach { folder ->
            folderDao.upsert(folder)
            inserted = true
        }
        tagsToInsert.forEach { tag ->
            tagDao.upsert(tag)
            inserted = true
        }
        return inserted
    }

    private fun decode(content: String): PreloadData =
        json.decodeFromString(PreloadData.serializer(), content)
}
