package id.usecase.noted.presentation.components.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import id.usecase.noted.ui.theme.MotionTokens
import id.usecase.noted.ui.theme.NotedTheme

@Composable
fun NoteEngagementBar(
    loveCount: Int,
    hasLovedByMe: Boolean,
    commentCount: Int,
    onLoveClick: () -> Unit,
    onCommentsClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            EngagementAction(
                count = loveCount,
                icon = {
                    LoveIcon(hasLovedByMe = hasLovedByMe)
                },
                contentDescription = if (hasLovedByMe) "Hapus love" else "Beri love",
                onClick = onLoveClick,
                enabled = enabled,
            )
            EngagementAction(
                count = commentCount,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ModeComment,
                        contentDescription = null,
                    )
                },
                contentDescription = "Lihat komentar",
                onClick = onCommentsClick,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun EngagementAction(
    count: Int,
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.semantics {
                this.contentDescription = contentDescription
            },
            onClick = onClick,
            enabled = enabled,
        ) {
            icon()
        }

        AnimatedContent(
            targetState = count,
            label = "engagement_count",
        ) { currentCount ->
            Text(
                text = currentCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun LoveIcon(
    hasLovedByMe: Boolean,
) {
    val loveScale by animateFloatAsState(
        targetValue = if (hasLovedByMe) 1.12f else 1f,
        animationSpec = MotionTokens.quickSpring,
        label = "love_icon_scale",
    )
    val loveTint by animateColorAsState(
        targetValue = if (hasLovedByMe) MaterialTheme.colorScheme.error else Color.Unspecified,
        animationSpec = MotionTokens.colorTransitionSpec(),
        label = "love_icon_tint",
    )

    Icon(
        imageVector = if (hasLovedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = null,
        modifier = Modifier.scale(loveScale),
        tint = loveTint,
    )
}

@Preview(showBackground = true)
@Composable
private fun NoteEngagementBarPreview() {
    NotedTheme {
        NoteEngagementBar(
            loveCount = 24,
            hasLovedByMe = true,
            commentCount = 8,
            onLoveClick = {},
            onCommentsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteEngagementBarDisabledPreview() {
    NotedTheme {
        NoteEngagementBar(
            loveCount = 0,
            hasLovedByMe = false,
            commentCount = 0,
            onLoveClick = {},
            onCommentsClick = {},
            enabled = false,
        )
    }
}
