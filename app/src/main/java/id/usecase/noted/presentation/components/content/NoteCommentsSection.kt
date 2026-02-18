package id.usecase.noted.presentation.components.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.usecase.noted.presentation.components.feedback.EmptyState
import id.usecase.noted.ui.theme.NotedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NoteCommentItem(
    val id: String,
    val authorUsername: String,
    val content: String,
    val createdAtEpochMillis: Long,
)

@Composable
fun NoteCommentsSection(
    comments: List<NoteCommentItem>,
    commentInput: String,
    isCommentsLoading: Boolean,
    isSendingComment: Boolean,
    hasMoreComments: Boolean,
    onCommentInputChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onRetryComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Komentar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            OutlinedTextField(
                value = commentInput,
                onValueChange = onCommentInputChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tulis komentar") },
                enabled = !isSendingComment,
                maxLines = 4,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onSubmitComment,
                    enabled = !isSendingComment,
                ) {
                    if (isSendingComment) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Kirim")
                }
            }

            if (isCommentsLoading && comments.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (comments.isEmpty()) {
                EmptyState(
                    message = "Belum ada komentar",
                    description = "Jadilah yang pertama berkomentar.",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    comments.forEach { comment ->
                        NoteCommentCard(comment = comment)
                    }
                }
            }

            AnimatedVisibility(visible = hasMoreComments || isCommentsLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onLoadMoreComments,
                        enabled = !isCommentsLoading,
                    ) {
                        Text(if (isCommentsLoading) "Memuat..." else "Muat komentar sebelumnya")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onRetryComments) {
                    Text("Muat ulang komentar")
                }
            }
        }
    }
}

@Composable
private fun NoteCommentCard(
    comment: NoteCommentItem,
) {
    val formattedTime = remember(comment.createdAtEpochMillis) {
        SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(comment.createdAtEpochMillis))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "@${comment.authorUsername}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCommentsSectionPreview() {
    NotedTheme {
        NoteCommentsSection(
            comments = listOf(
                NoteCommentItem(
                    id = "1",
                    authorUsername = "andi",
                    content = "Ini komentar pertama",
                    createdAtEpochMillis = 1_000L,
                ),
                NoteCommentItem(
                    id = "2",
                    authorUsername = "budi",
                    content = "Ini komentar kedua yang lebih panjang untuk melihat wrapping text.",
                    createdAtEpochMillis = 2_000L,
                ),
            ),
            commentInput = "",
            isCommentsLoading = false,
            isSendingComment = false,
            hasMoreComments = true,
            onCommentInputChanged = {},
            onSubmitComment = {},
            onLoadMoreComments = {},
            onRetryComments = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCommentsSectionLoadingPreview() {
    NotedTheme {
        NoteCommentsSection(
            comments = emptyList(),
            commentInput = "",
            isCommentsLoading = true,
            isSendingComment = false,
            hasMoreComments = false,
            onCommentInputChanged = {},
            onSubmitComment = {},
            onLoadMoreComments = {},
            onRetryComments = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCommentsSectionLongListPreview() {
    NotedTheme {
        NoteCommentsSection(
            comments = (1..8).map { index ->
                NoteCommentItem(
                    id = index.toString(),
                    authorUsername = "user$index",
                    content = "Komentar ke-$index",
                    createdAtEpochMillis = index * 1_000L,
                )
            },
            commentInput = "Komentar saya",
            isCommentsLoading = false,
            isSendingComment = false,
            hasMoreComments = false,
            onCommentInputChanged = {},
            onSubmitComment = {},
            onLoadMoreComments = {},
            onRetryComments = {},
        )
    }
}
