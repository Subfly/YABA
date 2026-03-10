package dev.subfly.yaba.ui.creation.bookmark.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.ShimmerItem
import dev.subfly.yaba.core.components.TagsRowContent
import dev.subfly.yaba.ui.creation.bookmark.model.BookmarkPreviewData
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_description_placeholder
import yaba.composeapp.generated.resources.bookmark_no_tags_added_title
import yaba.composeapp.generated.resources.bookmark_title_placeholder

private enum class PreviewSharedElementKey {
    Image,
    Title,
    Description,
    TagsRow,
    DomainImage,
}

private data class PreviewAnimationKey(
    val appearance: BookmarkAppearance,
    val cardSizing: CardImageSizing,
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BookmarkPreviewCard(
    data: BookmarkPreviewData,
    bookmarkAppearance: BookmarkAppearance,
    cardImageSizing: CardImageSizing,
    onClick: () -> Unit,
) {
    val color by remember(data.selectedFolder) {
        mutableStateOf(data.selectedFolder?.color ?: YabaColor.BLUE)
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = PreviewAnimationKey(
                appearance = bookmarkAppearance,
                cardSizing = cardImageSizing,
            ),
        ) { target ->
            when (target.appearance) {
                BookmarkAppearance.LIST -> ListPreview(
                    data = data,
                    color = color,
                    onClick = onClick,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent,
                )

                BookmarkAppearance.CARD -> when (target.cardSizing) {
                    CardImageSizing.BIG -> CardBigImagePreview(
                        data = data,
                        color = color,
                        onClick = onClick,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                    )

                    CardImageSizing.SMALL -> CardSmallImagePreview(
                        data = data,
                        color = color,
                        onClick = onClick,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                    )
                }

                BookmarkAppearance.GRID -> GridPreview(
                    data = data,
                    color = color,
                    onClick = onClick,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ListPreview(
    data: BookmarkPreviewData,
    color: YabaColor,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    with(sharedTransitionScope) {
        SegmentedListItem(
            modifier = Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)),
            onClick = onClick,
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            content = {
                ShimmerItem(
                    isLoading = data.isLoading,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Title),
                        animatedVisibilityScope = animatedContentScope,
                    ),
                ) {
                    Text(
                        text = data.label.ifBlank { stringResource(Res.string.bookmark_title_placeholder) },
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            supportingContent = {
                ShimmerItem(
                    isLoading = data.isLoading,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Description),
                        animatedVisibilityScope = animatedContentScope,
                    ),
                ) {
                    Text(
                        text = data.description.ifBlank { stringResource(Res.string.bookmark_description_placeholder) },
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Image),
                        animatedVisibilityScope = animatedContentScope,
                    )
                ) {
                    if (data.imageData == null) {
                        ShimmerItem(
                            isLoading = data.isLoading,
                            modifier = Modifier.size(64.dp),
                            cornerRadius = 12.dp,
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                            ) {
                                YabaIcon(
                                    modifier = Modifier.padding(16.dp),
                                    name = data.emptyImageIconName,
                                    color = color,
                                )
                            }
                        }
                    } else {
                        ShimmerItem(
                            isLoading = data.isLoading,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                            cornerRadius = 12.dp,
                        ) {
                            YabaImage(
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                                bytes = data.imageData,
                            )
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun CardBigImagePreview(
    data: BookmarkPreviewData,
    color: YabaColor,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    with(sharedTransitionScope) {
        Surface(
            modifier = Modifier.padding(horizontal = 12.dp)
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
                    modifier = Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Image),
                        animatedVisibilityScope = animatedContentScope,
                    )
                ) {
                    if (data.imageData == null) {
                        ShimmerItem(
                            isLoading = data.isLoading,
                            modifier = Modifier.fillMaxWidth().height(128.dp),
                            cornerRadius = 12.dp,
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(128.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                            ) {
                                YabaIcon(
                                    modifier = Modifier.padding(32.dp),
                                    name = data.emptyImageIconName,
                                    color = color,
                                )
                            }
                        }
                    } else {
                        ShimmerItem(
                            isLoading = data.isLoading,
                            modifier = Modifier.fillMaxWidth().height(128.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            cornerRadius = 12.dp,
                        ) {
                            YabaImage(
                                modifier = Modifier.fillMaxWidth().height(128.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                bytes = data.imageData,
                            )
                        }
                    }
                }
                ShimmerItem(
                    isLoading = data.isLoading,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Title),
                        animatedVisibilityScope = animatedContentScope,
                    ),
                ) {
                    Text(
                        text = data.label.ifBlank { stringResource(Res.string.bookmark_title_placeholder) },
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ShimmerItem(
                    isLoading = data.isLoading,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Description),
                        animatedVisibilityScope = animatedContentScope,
                    ),
                ) {
                    Text(
                        text = data.description.ifBlank { stringResource(Res.string.bookmark_description_placeholder) },
                        maxLines = 3,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ShimmerItem(
                        isLoading = data.isLoading,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.TagsRow),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    ) {
                        TagsRowContent(
                            tags = data.selectedTags,
                            emptyStateTextRes = Res.string.bookmark_no_tags_added_title,
                            emptyStateColor = color,
                        )
                    }
                    if (data.domainImageData != null) {
                        ShimmerItem(
                            isLoading = data.isLoading,
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.DomainImage),
                                animatedVisibilityScope = animatedContentScope,
                            ),
                        ) {
                            YabaImage(
                                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)),
                                bytes = data.domainImageData,
                            )
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
    data: BookmarkPreviewData,
    color: YabaColor,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    with(sharedTransitionScope) {
        Surface(
            modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Image),
                            animatedVisibilityScope = animatedContentScope,
                        )
                    ) {
                        if (data.imageData == null) {
                            ShimmerItem(
                                isLoading = data.isLoading,
                                modifier = Modifier.size(64.dp),
                                cornerRadius = 12.dp,
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                                ) {
                                    YabaIcon(
                                        modifier = Modifier.padding(16.dp),
                                        name = data.emptyImageIconName,
                                        color = color,
                                    )
                                }
                            }
                        } else {
                            ShimmerItem(
                                isLoading = data.isLoading,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                                cornerRadius = 12.dp,
                            ) {
                                YabaImage(
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                                    bytes = data.imageData,
                                )
                            }
                        }
                    }
                    ShimmerItem(
                        isLoading = data.isLoading,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Title),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    ) {
                        Text(
                            text = data.label.ifBlank { stringResource(Res.string.bookmark_title_placeholder) },
                            maxLines = 2,
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                ShimmerItem(
                    isLoading = data.isLoading,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Description),
                        animatedVisibilityScope = animatedContentScope,
                    ),
                ) {
                    Text(
                        text = data.description.ifBlank { stringResource(Res.string.bookmark_description_placeholder) },
                        maxLines = 3,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ShimmerItem(
                        isLoading = data.isLoading,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.TagsRow),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    ) {
                        TagsRowContent(
                            tags = data.selectedTags,
                            emptyStateTextRes = Res.string.bookmark_no_tags_added_title,
                            emptyStateColor = color,
                        )
                    }
                    if (data.domainImageData != null) {
                        ShimmerItem(
                            isLoading = data.isLoading,
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.DomainImage),
                                animatedVisibilityScope = animatedContentScope,
                            ),
                        ) {
                            YabaImage(
                                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)),
                                bytes = data.domainImageData,
                            )
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
    data: BookmarkPreviewData,
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
                modifier = Modifier.width(200.dp)
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
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Image),
                            animatedVisibilityScope = animatedContentScope,
                        )
                    ) {
                        if (data.imageData == null) {
                            ShimmerItem(
                                isLoading = data.isLoading,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                cornerRadius = 12.dp,
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(color.iconTintArgb()).copy(alpha = 0.3F),
                                ) {
                                    YabaIcon(
                                        modifier = Modifier.padding(52.dp),
                                        name = data.emptyImageIconName,
                                        color = color,
                                    )
                                }
                            }
                        } else {
                            ShimmerItem(
                                isLoading = data.isLoading,
                                modifier = Modifier.fillMaxWidth().height(128.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                cornerRadius = 12.dp,
                            ) {
                                YabaImage(
                                    modifier = Modifier.fillMaxWidth().height(128.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    bytes = data.imageData,
                                )
                            }
                        }
                    }
                    ShimmerItem(
                        isLoading = data.isLoading,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Title),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    ) {
                        Text(
                            text = data.label.ifBlank { stringResource(Res.string.bookmark_title_placeholder) },
                            maxLines = 2,
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    ShimmerItem(
                        isLoading = data.isLoading,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = PreviewSharedElementKey.Description),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    ) {
                        Text(
                            text = data.description.ifBlank { stringResource(Res.string.bookmark_description_placeholder) },
                            maxLines = 3,
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
