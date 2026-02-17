package id.usecase.noted.presentation.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Base card component for note items with consistent styling.
 *
 * @param modifier Modifier to be applied
 * @param onClick Callback when card is clicked
 * @param content Card content
 */
@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cardModifier = modifier.fillMaxWidth()
    
    if (onClick != null) {
        Card(
            modifier = cardModifier,
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

/**
 * Note card with elevated appearance for featured items.
 *
 * @param modifier Modifier to be applied
 * @param onClick Callback when card is clicked
 * @param content Card content
 */
@Composable
fun NoteCardElevated(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCardPreview() {
    NotedTheme {
        NoteCard(
            onClick = {}
        ) {
            Text(
                text = "Judul Catatan",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Isi catatan akan ditampilkan di sini...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCardElevatedPreview() {
    NotedTheme {
        NoteCardElevated(
            onClick = {}
        ) {
            Text(
                text = "Catatan Unggulan",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Catatan dengan tampilan elevated",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteCardStaticPreview() {
    NotedTheme {
        NoteCard {
            Text(
                text = "Catatan Non-Clickable",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
