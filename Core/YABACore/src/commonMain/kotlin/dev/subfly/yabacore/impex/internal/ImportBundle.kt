package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.impex.model.TagLink

/**
 * Internal representation of imported data before being applied to the system.
 *
 * The ImportBundle is converted to filesystem JSON files and SQLite cache
 * entries using the filesystem-first approach in ImportExportManager.
 */
internal data class ImportBundle(
    val folders: List<FolderDomainModel> = emptyList(),
    val tags: List<TagDomainModel> = emptyList(),
    val bookmarks: List<LinkBookmarkDomainModel> = emptyList(),
    val tagLinks: List<TagLink> = emptyList(),
)
