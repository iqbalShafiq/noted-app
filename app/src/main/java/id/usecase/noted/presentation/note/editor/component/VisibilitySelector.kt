package id.usecase.noted.presentation.note.editor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.usecase.noted.domain.NoteVisibility

@Composable
fun VisibilitySelector(
    currentVisibility: NoteVisibility,
    onVisibilityChange: (NoteVisibility) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = NoteVisibility.entries

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { visibility ->
            VisibilityChip(
                visibility = visibility,
                isSelected = visibility == currentVisibility,
                onClick = { onVisibilityChange(visibility) },
            )
        }
    }
}

@Composable
private fun VisibilityChip(
    visibility: NoteVisibility,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val (icon, label) = when (visibility) {
        NoteVisibility.PRIVATE -> Pair(Icons.Outlined.Lock, "Private")
        NoteVisibility.LINK_SHARED -> Pair(Icons.Filled.Link, "Link Shared")
        NoteVisibility.PUBLIC -> Pair(Icons.Filled.Public, "Public")
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
