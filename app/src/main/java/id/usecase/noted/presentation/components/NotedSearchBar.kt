package id.usecase.noted.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val TAB_TITLES = listOf("Note Saya", "Tersimpan", "Riwayat")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
    searchHistory: List<String>,
    searchSelectedTab: Int,
    onSearchTabSelected: (Int) -> Unit,
    searchResults: List<SearchResultItem>,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    SearchBar(
        modifier = modifier.fillMaxWidth(),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    onSearch()
                    expanded = false
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                placeholder = { Text("Cari note...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Tutup pencarian",
                        )
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // Search History Section
            if (searchHistory.isNotEmpty() && query.isBlank()) {
                Text(
                    text = "Riwayat Pencarian",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                searchHistory.take(5).forEach { historyItem ->
                    ListItem(
                        headlineContent = { Text(historyItem) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier
                            .clickable {
                                onQueryChange(historyItem)
                            }
                            .fillMaxWidth(),
                    )
                }
            }

            // Tabs for search results
            if (query.isNotBlank()) {
                TabRow(
                    selectedTabIndex = searchSelectedTab,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TAB_TITLES.forEachIndexed { index, title ->
                        Tab(
                            selected = searchSelectedTab == index,
                            onClick = { onSearchTabSelected(index) },
                            text = { Text(title) },
                        )
                    }
                }

                // Search Results
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Tidak ada note yang cocok",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    searchResults.forEach { note ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = note.content.take(100),
                                    maxLines = 2,
                                )
                            },
                            modifier = Modifier
                                .clickable { onNoteClick(note.id) }
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

data class SearchResultItem(
    val id: Long,
    val content: String,
)
