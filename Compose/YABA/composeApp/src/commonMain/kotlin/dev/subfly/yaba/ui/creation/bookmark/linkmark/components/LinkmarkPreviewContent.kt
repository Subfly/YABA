package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.model.utils.uiTitle
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_description_placeholder
import yaba.composeapp.generated.resources.bookmark_no_tags_added_title
import yaba.composeapp.generated.resources.bookmark_title_placeholder
import yaba.composeapp.generated.resources.preview

/**
 * Shared element keys for smooth transitions between preview types
 */
private enum class PreviewSharedElementKey {
    Image,
    Title,
    Description,
    TagsRow
}

/**
 * Composite key for AnimatedContent to track both appearance and card sizing changes
 */
private data class PreviewAnimationKey(
    val appearance: ContentAppearance,
    val cardSizing: CardImageSizing,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkmarkPreviewContent(
    state: LinkmarkCreationUIState,
    onChangePreviewType: () -> Unit,
    onOpenImageSelector: () -> Unit,
) {
    val color by remember(state.selectedFolder) {
        mutableStateOf(state.selectedFolder?.color ?: YabaColor.BLUE)
    }

    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.preview),
        iconName = "image-03",
        extraContent = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onChangePreviewType,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YabaIcon(
                        name = state.contentAppearance.uiIconName(),
                        color = state.selectedFolder?.color ?: YabaColor.BLUE,
                    )
                    Text(
                        text = state.contentAppearance.uiTitle(),
                        color = Color(color.iconTintArgb()),
                    )

                    if (state.contentAppearance == ContentAppearance.CARD) {
                        YabaIcon(
                            name = state.cardImageSizing.uiIconName(),
                            color = Color(color.iconTintArgb()),
                        )
                        Text(
                            text = state.cardImageSizing.uiTitle(),
                            color = Color(color.iconTintArgb()),
                        )
                    }
                }
            }
        }
    )
    Spacer(modifier = Modifier.height(12.dp))
    PreviewContent(
        state = state,
        onClick = onOpenImageSelector,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PreviewContent(
    state: LinkmarkCreationUIState,
    onClick: () -> Unit,
) {
    val color by remember(state.selectedFolder) {
        mutableStateOf(state.selectedFolder?.color ?: YabaColor.BLUE)
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = PreviewAnimationKey(
                appearance = state.contentAppearance,
                cardSizing = state.cardImageSizing,
            ),
        ) { target ->
            when (target.appearance) {
                ContentAppearance.LIST -> {
                    ListPreview(
                        state = state,
                        color = color,
                        onClick = onClick,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                    )
                }

                ContentAppearance.CARD -> {
                    when (target.cardSizing) {
                        CardImageSizing.BIG -> {
                            CardBigImagePreview(
                                state = state,
                                color = color,
                                onClick = onClick,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedContentScope = this@AnimatedContent,
                            )
                        }

                        CardImageSizing.SMALL -> {
                            CardSmallImagePreview(
                                state = state,
                                color = color,
                                onClick = onClick,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedContentScope = this@AnimatedContent,
                            )
                        }
                    }
                }

                ContentAppearance.GRID -> {
                    GridPreview(
                        state = state,
                        color = color,
                        onClick = onClick,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ListPreview(
    state: LinkmarkCreationUIState,
    color: YabaColor,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    with(sharedTransitionScope) {
        ListItem(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            headlineContent = {
                Text(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.Title
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    text = state.label.ifBlank {
                        stringResource(Res.string.bookmark_title_placeholder)
                    },
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.Description
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    text = state.description.ifBlank {
                        stringResource(Res.string.bookmark_description_placeholder)
                    },
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.Image
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        )
                ) {
                    if (state.imageData == null) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(16.dp),
                                name = state.selectedLinkType.uiIconName(),
                                color = color,
                            )
                        }
                    } else {
                        YabaImage(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            bytes = state.imageData,
                        )
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun CardBigImagePreview(
    state: LinkmarkCreationUIState,
    color: YabaColor,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    with(sharedTransitionScope) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.Image
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        )
                ) {
                    if (state.imageData == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(128.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(32.dp),
                                name = state.selectedLinkType.uiIconName(),
                                color = color,
                            )
                        }
                    } else {
                        YabaImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(128.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            bytes = state.imageData,
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.Title
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    text = state.label.ifBlank {
                        stringResource(Res.string.bookmark_title_placeholder)
                    },
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.Description
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    text = state.description.ifBlank {
                        stringResource(Res.string.bookmark_description_placeholder)
                    },
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.TagsRow
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.selectedTags.isEmpty()) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(4.dp),
                                name = "tags",
                                color = color,
                            )
                        }
                        Text(
                            text = stringResource(Res.string.bookmark_no_tags_added_title),
                            fontStyle = FontStyle.Italic,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        state.selectedTags.fastForEach {

                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun CardSmallImagePreview(
    state: LinkmarkCreationUIState,
    color: YabaColor,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    with(sharedTransitionScope) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(
                                    key = PreviewSharedElementKey.Image
                                ),
                                animatedVisibilityScope = animatedContentScope,
                            )
                    ) {
                        if (state.imageData == null) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                            ) {
                                YabaIcon(
                                    modifier = Modifier.padding(16.dp),
                                    name = state.selectedLinkType.uiIconName(),
                                    color = color,
                                )
                            }
                        } else {
                            YabaImage(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                bytes = state.imageData,
                            )
                        }
                    }
                    Text(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = PreviewSharedElementKey.Title
                                ),
                                animatedVisibilityScope = animatedContentScope,
                            ),
                        text = state.label.ifBlank {
                            stringResource(Res.string.bookmark_title_placeholder)
                        },
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.Description
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    text = state.description.ifBlank {
                        stringResource(Res.string.bookmark_description_placeholder)
                    },
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = PreviewSharedElementKey.TagsRow
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.selectedTags.isEmpty()) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(4.dp),
                                name = "tags",
                                color = color,
                            )
                        }
                        Text(
                            text = stringResource(Res.string.bookmark_no_tags_added_title),
                            fontStyle = FontStyle.Italic,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        state.selectedTags.fastForEach {

                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun GridPreview(
    state: LinkmarkCreationUIState,
    color: YabaColor,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    with(sharedTransitionScope) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(width = 200.dp)
                    .heightIn(min = 200.dp, max = 560.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(
                                    key = PreviewSharedElementKey.Image
                                ),
                                animatedVisibilityScope = animatedContentScope,
                            )
                    ) {
                        if (state.imageData == null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                            ) {
                                YabaIcon(
                                    modifier = Modifier.padding(52.dp),
                                    name = state.selectedLinkType.uiIconName(),
                                    color = color,
                                )
                            }
                        } else {
                            YabaImage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(128.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                bytes = state.imageData,
                            )
                        }
                    }
                    Text(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = PreviewSharedElementKey.Title
                                ),
                                animatedVisibilityScope = animatedContentScope,
                            ),
                        text = state.label.ifBlank {
                            stringResource(Res.string.bookmark_title_placeholder)
                        },
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = PreviewSharedElementKey.Description
                                ),
                                animatedVisibilityScope = animatedContentScope,
                            ),
                        text = state.description.ifBlank {
                            stringResource(Res.string.bookmark_description_placeholder)
                        },
                        maxLines = 3,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
