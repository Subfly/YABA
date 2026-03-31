package dev.subfly.yaba.util

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.runtime.Composable
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.ContentAppearance
import dev.subfly.yaba.core.model.utils.FabPosition
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.model.utils.ThemePreference

/**
 * Composable extension functions for UI titles.
 * These replace the platform-specific uiTitle() functions that were in Core.
 * All strings are localized via compose resources.
 */

@Composable
fun SortType.uiTitle(): String =
    when (this) {
        SortType.CREATED_AT -> stringResource(R.string.sort_created_at)
        SortType.EDITED_AT -> stringResource(R.string.sort_edited_at)
        SortType.LABEL -> stringResource(R.string.sort_label)
    }

@Composable
fun SortOrderType.uiTitle(): String =
    when (this) {
        SortOrderType.ASCENDING -> stringResource(R.string.sort_order_ascending)
        SortOrderType.DESCENDING -> stringResource(R.string.sort_order_descending)
    }

@Composable
fun FabPosition.uiTitle(): String =
    when (this) {
        FabPosition.LEFT -> stringResource(R.string.fab_left_aligned)
        FabPosition.RIGHT -> stringResource(R.string.fab_right_aligned)
        FabPosition.CENTER -> stringResource(R.string.fab_centered)
    }

@Composable
fun ThemePreference.uiTitle(): String =
    when (this) {
        ThemePreference.LIGHT -> stringResource(R.string.theme_light)
        ThemePreference.DARK -> stringResource(R.string.theme_dark)
        ThemePreference.SYSTEM -> stringResource(R.string.theme_system)
    }

@Deprecated("Use BookmarkAppearance instead")
@Composable
fun ContentAppearance.uiTitle(): String =
    when (this) {
        ContentAppearance.LIST -> stringResource(R.string.view_list)
        ContentAppearance.CARD -> stringResource(R.string.view_card)
        ContentAppearance.GRID -> stringResource(R.string.view_grid)
    }

@Composable
fun BookmarkAppearance.uiTitle(): String =
    when (this) {
        BookmarkAppearance.LIST -> stringResource(R.string.view_list)
        BookmarkAppearance.CARD -> stringResource(R.string.view_card)
        BookmarkAppearance.GRID -> stringResource(R.string.view_grid)
    }

@Composable
fun CardImageSizing.uiTitle(): String =
    when (this) {
        CardImageSizing.BIG -> stringResource(R.string.card_image_sizing_big)
        CardImageSizing.SMALL -> stringResource(R.string.card_image_sizing_small)
    }
