package id.usecase.noted.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.usecase.noted.ui.theme.NotedTheme

/**
 * Default configurations for NotedSearchBar component.
 */
@OptIn(ExperimentalMaterial3Api::class)
object NotedSearchBarDefaults {

    /**
     * Creates input field configuration for the search bar.
     *
     * @param query Current search query text
     * @param onQueryChange Callback when query text changes
     * @param onSearch Callback when search is submitted
     * @param expanded Whether the search bar is expanded
     * @param onExpandedChange Callback when expanded state changes
     * @param placeholder Placeholder text to display when query is empty
     * @param onDismiss Callback when search is dismissed/cleared
     * @param modifier Modifier for the input field
     */
    @Composable
    fun inputField(
        query: String,
        onQueryChange: (String) -> Unit,
        onSearch: () -> Unit,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        placeholder: String = "Cari...",
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier,
    ) = SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { onSearch() },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty() || expanded) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Tutup pencarian",
                    )
                }
            }
        },
        modifier = modifier,
    )

    /**
     * Default colors for the search bar using MaterialTheme.
     */
    val colors: SearchBarColors
        @Composable
        get() = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            dividerColor = MaterialTheme.colorScheme.outlineVariant,
        )
}

/**
 * A reusable Material3 SearchBar component for the Noted app.
 *
 * This component wraps Material3 SearchBar with sensible defaults and proper
 * accessibility support. It supports both collapsed and expanded states.
 *
 * @param query Current search query text
 * @param onQueryChange Callback when query text changes
 * @param onSearch Callback when search is submitted
 * @param onDismiss Callback when search is dismissed/cleared
 * @param expanded Whether the search bar is expanded
 * @param onExpandedChange Callback when expanded state changes
 * @param modifier Modifier for the search bar
 * @param placeholder Placeholder text to display when query is empty
 * @param content Content to display when the search bar is expanded (search suggestions, results, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari...",
    content: @Composable ColumnScope.() -> Unit,
) {
    SearchBar(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Search bar" },
        inputField = {
            NotedSearchBarDefaults.inputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = placeholder,
                onDismiss = {
                    onQueryChange("")
                    onDismiss()
                },
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        colors = NotedSearchBarDefaults.colors,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NotedSearchBarCollapsedPreview() {
    NotedTheme {
        NotedSearchBar(
            query = "",
            onQueryChange = {},
            onSearch = {},
            onDismiss = {},
            expanded = false,
            onExpandedChange = {},
            content = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NotedSearchBarExpandedPreview() {
    NotedTheme {
        NotedSearchBar(
            query = "contoh pencarian",
            onQueryChange = {},
            onSearch = {},
            onDismiss = {},
            expanded = true,
            onExpandedChange = {},
            content = {
                Text(
                    text = "Hasil pencarian akan muncul di sini",
                    modifier = Modifier.padding(16.dp),
                )
            },
        )
    }
}
