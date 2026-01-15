package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.ThemePreference
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_type_audio
import yaba.composeapp.generated.resources.bookmark_type_image
import yaba.composeapp.generated.resources.bookmark_type_link
import yaba.composeapp.generated.resources.bookmark_type_music
import yaba.composeapp.generated.resources.bookmark_type_none
import yaba.composeapp.generated.resources.bookmark_type_video
import yaba.composeapp.generated.resources.card_image_sizing_big
import yaba.composeapp.generated.resources.card_image_sizing_small
import yaba.composeapp.generated.resources.fab_centered
import yaba.composeapp.generated.resources.fab_left_aligned
import yaba.composeapp.generated.resources.fab_right_aligned
import yaba.composeapp.generated.resources.sort_created_at
import yaba.composeapp.generated.resources.sort_custom
import yaba.composeapp.generated.resources.sort_edited_at
import yaba.composeapp.generated.resources.sort_label
import yaba.composeapp.generated.resources.sort_order_ascending
import yaba.composeapp.generated.resources.sort_order_descending
import yaba.composeapp.generated.resources.theme_dark
import yaba.composeapp.generated.resources.theme_light
import yaba.composeapp.generated.resources.theme_system
import yaba.composeapp.generated.resources.view_card
import yaba.composeapp.generated.resources.view_grid
import yaba.composeapp.generated.resources.view_list

/**
 * Composable extension functions for UI titles.
 * These replace the platform-specific uiTitle() functions that were in Core.
 * All strings are localized via compose resources.
 */

@Composable
fun SortType.uiTitle(): String =
    when (this) {
        SortType.CREATED_AT -> stringResource(Res.string.sort_created_at)
        SortType.EDITED_AT -> stringResource(Res.string.sort_edited_at)
        SortType.LABEL -> stringResource(Res.string.sort_label)
        SortType.CUSTOM -> stringResource(Res.string.sort_custom)
    }

@Composable
fun SortOrderType.uiTitle(): String =
    when (this) {
        SortOrderType.ASCENDING -> stringResource(Res.string.sort_order_ascending)
        SortOrderType.DESCENDING -> stringResource(Res.string.sort_order_descending)
    }

@Composable
fun FabPosition.uiTitle(): String =
    when (this) {
        FabPosition.LEFT -> stringResource(Res.string.fab_left_aligned)
        FabPosition.RIGHT -> stringResource(Res.string.fab_right_aligned)
        FabPosition.CENTER -> stringResource(Res.string.fab_centered)
    }

@Composable
fun ThemePreference.uiTitle(): String =
    when (this) {
        ThemePreference.LIGHT -> stringResource(Res.string.theme_light)
        ThemePreference.DARK -> stringResource(Res.string.theme_dark)
        ThemePreference.SYSTEM -> stringResource(Res.string.theme_system)
    }

@Deprecated("Use BookmarkAppearance instead")
@Composable
fun ContentAppearance.uiTitle(): String =
    when (this) {
        ContentAppearance.LIST -> stringResource(Res.string.view_list)
        ContentAppearance.CARD -> stringResource(Res.string.view_card)
        ContentAppearance.GRID -> stringResource(Res.string.view_grid)
    }

@Composable
fun BookmarkAppearance.uiTitle(): String =
    when (this) {
        BookmarkAppearance.LIST -> stringResource(Res.string.view_list)
        BookmarkAppearance.CARD -> stringResource(Res.string.view_card)
        BookmarkAppearance.GRID -> stringResource(Res.string.view_grid)
    }

@Composable
fun CardImageSizing.uiTitle(): String =
    when (this) {
        CardImageSizing.BIG -> stringResource(Res.string.card_image_sizing_big)
        CardImageSizing.SMALL -> stringResource(Res.string.card_image_sizing_small)
    }

@Composable
fun LinkType.uiTitle(): String =
    when (this) {
        LinkType.NONE -> stringResource(Res.string.bookmark_type_none)
        LinkType.WEB_LINK -> stringResource(Res.string.bookmark_type_link)
        LinkType.VIDEO -> stringResource(Res.string.bookmark_type_video)
        LinkType.IMAGE -> stringResource(Res.string.bookmark_type_image)
        LinkType.AUDIO -> stringResource(Res.string.bookmark_type_audio)
        LinkType.MUSIC -> stringResource(Res.string.bookmark_type_music)
    }
