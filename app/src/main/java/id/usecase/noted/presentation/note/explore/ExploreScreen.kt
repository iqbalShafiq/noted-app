package id.usecase.noted.presentation.note.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.usecase.noted.presentation.components.feedback.EmptyState
import id.usecase.noted.presentation.components.feedback.ErrorState
import id.usecase.noted.presentation.components.feedback.LoadingState
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
import id.usecase.noted.ui.theme.NotedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExploreScreenRoot(
    viewModel: ExploreViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ExploreEffect.NavigateBack -> onNavigateBack()
                is ExploreEffect.NavigateToNoteDetail -> onNavigateToNoteDetail(effect.noteId)
                is ExploreEffect.ShowMessage -> onShowMessage(effect.message)
            }
        }
    }

    ExploreScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    state: ExploreState,
    onIntent: (ExploreIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                NotedTopAppBar(
                    title = "Jelajahi",
                    onNavigateBack = { onIntent(ExploreIntent.NavigateBackClicked) },
                    actions = {
                        IconButton(onClick = { onIntent(ExploreIntent.SearchClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                when {
                    state.isLoading -> {
                        LoadingState()
                    }

                    state.errorMessage != null -> {
                        ErrorState(
                            message = state.errorMessage,
                            onRetry = { onIntent(ExploreIntent.LoadExploreNotes) },
                        )
                    }

                    state.notes.isEmpty() -> {
                        EmptyState(
                            message = "Belum ada note dari pengguna lain",
                        )
                    }

                    else -> {
                        val notesToDisplay = if (state.isSearchExpanded) {
                            state.filteredNotes
                        } else {
                            state.notes
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                        ) {
                            items(
                                items = notesToDisplay,
                                key = { item -> item.id },
                            ) { item ->
                                ExploreNoteItem(
                                    note = item,
                                    onClick = { onIntent(ExploreIntent.NoteClicked(item.id)) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search overlay - positioned outside Scaffold to cover entire screen including TopAppBar
        if (state.isSearchExpanded) {
            ExploreSearchBar(
                query = state.searchQuery,
                onQueryChange = { onIntent(ExploreIntent.SearchQueryChanged(it)) },
                onDismiss = { onIntent(ExploreIntent.DismissSearch) },
                searchHistory = state.searchHistory,
                filteredNotes = state.filteredNotes,
                onNoteClick = { onIntent(ExploreIntent.NoteClicked(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ExploreNoteItem(
    note: ExploreNoteUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(note.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.createdAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Oleh: ${note.ownerUserId.take(8)}...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreScreenPreview() {
    NotedTheme {
        ExploreScreen(
            state = ExploreState(
                isLoading = false,
                notes = listOf(
                    ExploreNoteUi(
                        id = "1",
                        content = "Note dari pengguna lain",
                        ownerUserId = "user123",
                        createdAt = System.currentTimeMillis(),
                    ),
                    ExploreNoteUi(
                        id = "2",
                        content = "Note menarik lainnya",
                        ownerUserId = "user456",
                        createdAt = System.currentTimeMillis() - 3600000,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreScreenEmptyPreview() {
    NotedTheme {
        ExploreScreen(
            state = ExploreState(
                isLoading = false,
                notes = emptyList(),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreScreenLoadingPreview() {
    NotedTheme {
        ExploreScreen(
            state = ExploreState(
                isLoading = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreScreenSearchExpandedPreview() {
    NotedTheme {
        ExploreScreen(
            state = ExploreState(
                isLoading = false,
                isSearchExpanded = true,
                searchQuery = "",
                searchHistory = listOf("kotlin", "compose", "android"),
                notes = listOf(
                    ExploreNoteUi(
                        id = "1",
                        content = "Note dari pengguna lain",
                        ownerUserId = "user123",
                        createdAt = System.currentTimeMillis(),
                    ),
                    ExploreNoteUi(
                        id = "2",
                        content = "Note menarik lainnya",
                        ownerUserId = "user456",
                        createdAt = System.currentTimeMillis() - 3600000,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    searchHistory: List<String>,
    filteredNotes: List<ExploreNoteUi>,
    onNoteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    SearchBar(
        modifier = modifier,
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { expanded = false },
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
            modifier = Modifier.fillMaxWidth()
        ) {
            if (searchHistory.isNotEmpty() && query.isBlank()) {
                Text(
                    text = "Riwayat Pencarian",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
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
                        modifier = Modifier.clickable {
                            onQueryChange(historyItem)
                        },
                    )
                }
            }

            if (query.isNotBlank()) {
                if (filteredNotes.isEmpty()) {
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
                    Text(
                        text = "Hasil Pencarian (${filteredNotes.size})",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    filteredNotes.forEach { note ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = note.content.take(100),
                                    maxLines = 2,
                                )
                            },
                            modifier = Modifier.clickable {
                                onNoteClick(note.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
