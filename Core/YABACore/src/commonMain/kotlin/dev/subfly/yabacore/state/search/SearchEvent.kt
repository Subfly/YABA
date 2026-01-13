package dev.subfly.yabacore.state.search

import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed interface SearchEvent {
    data object OnInit : SearchEvent
    data class OnChangeQuery(val query: String) : SearchEvent
    data class OnToggleFolderFilter(val folderId: Uuid) : SearchEvent
    data class OnToggleTagFilter(val tagId: Uuid) : SearchEvent
    data class OnChangeSort(val sortType: SortType, val sortOrder: SortOrderType) : SearchEvent
    data class OnChangeAppearance(val appearance: BookmarkAppearance) : SearchEvent
}
