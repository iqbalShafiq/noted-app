package id.usecase.noted.presentation.note.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.usecase.noted.domain.NoteVisibility
import id.usecase.noted.presentation.note.list.NoteListItemUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteListItem(
    note: NoteListItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(note.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.createdAt))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val visibilityIcon = when (note.visibility) {
                    NoteVisibility.PRIVATE -> Icons.Default.Lock
                    NoteVisibility.LINK_SHARED -> Icons.Default.Link
                    NoteVisibility.PUBLIC -> Icons.Default.Public
                }
                val iconTint = when (note.visibility) {
                    NoteVisibility.PRIVATE -> MaterialTheme.colorScheme.onSurfaceVariant
                    NoteVisibility.LINK_SHARED -> MaterialTheme.colorScheme.primary
                    NoteVisibility.PUBLIC -> MaterialTheme.colorScheme.primary
                }
                Icon(
                    imageVector = visibilityIcon,
                    contentDescription = note.visibility.toString(),
                    tint = iconTint,
                )
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
