package id.usecase.noted.presentation.note.list

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.usecase.noted.presentation.components.NotedSearchBar
import id.usecase.noted.presentation.components.SearchResultItem
import id.usecase.noted.presentation.note.list.component.NoteHistoryListItem
import id.usecase.noted.presentation.note.list.component.NoteListItem
import id.usecase.noted.presentation.note.list.preview.NoteListPreviewData
import id.usecase.noted.ui.theme.NotedTheme

private val TAB_TITLES = listOf("Note Saya", "Tersimpan", "Riwayat")

@Composable
fun NoteListScreenRoot(
    viewModel: NoteListViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NoteListEffect.NavigateToEditor -> onNavigateToEditor(effect.noteId)
                is NoteListEffect.NavigateToHistoryNote -> onNavigateToNoteDetail(effect.noteId)
                is NoteListEffect.ShowMessage -> onShowMessage(effect.message)
                NoteListEffect.NavigateToSync -> onNavigateToSync()
                NoteListEffect.NavigateToAccount -> onNavigateToAccount()
                NoteListEffect.NavigateToExplore -> onNavigateToExplore()
            }
        }
    }

    NoteListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    state: NoteListState,
    onIntent: (NoteListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchSelectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Noted") },
                    actions = {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari note",
                            )
                        }
                    },
                )
            },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { onIntent(NoteListIntent.ExploreClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Jelajahi",
                        )
                    }
                    IconButton(onClick = { onIntent(NoteListIntent.SyncClicked) }) {
                        BadgedBox(
                            badge = {
                                if (state.syncStatus.pendingUploadCount > 0) {
                                    Badge {
                                        Text(
                                            text = if (state.syncStatus.pendingUploadCount > 99) {
                                                "99+"
                                            } else {
                                                state.syncStatus.pendingUploadCount.toString()
                                            },
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sinkronisasi",
                            )
                        }
                    }
                    IconButton(onClick = { onIntent(NoteListIntent.AccountClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Akun",
                        )
                    }
                },
                floatingActionButton = {
                    if (state.selectedTab != 2) {
                        FloatingActionButton(
                            onClick = { onIntent(NoteListIntent.AddNoteClicked) },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Note",
                            )
                        }
                    }
                },
            )
        },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                TabRow(
                    selectedTabIndex = state.selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TAB_TITLES.forEachIndexed { index, title ->
                        Tab(
                            selected = state.selectedTab == index,
                            onClick = { onIntent(NoteListIntent.TabSelected(index)) },
                            text = { Text(title) },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    when {
                        state.isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        state.errorMessage != null -> {
                            ErrorState(
                                errorMessage = state.errorMessage,
                                onRetry = { onIntent(NoteListIntent.RetryObserve) },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }

                        state.isCurrentTabEmpty -> {
                            EmptyState(
                                selectedTab = state.selectedTab,
                                isSearchActive = false,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }

                        else -> {
                            when (state.selectedTab) {
                                0 -> NoteListContent(
                                    notes = state.filteredMyNotes,
                                    onNoteClick = { noteId ->
                                        onIntent(NoteListIntent.NoteClicked(noteId = noteId))
                                    },
                                )
                                1 -> NoteListContent(
                                    notes = state.filteredSavedNotes,
                                    onNoteClick = { noteId ->
                                        onIntent(NoteListIntent.NoteClicked(noteId = noteId))
                                    },
                                )
                                2 -> HistoryListContent(
                                    historyNotes = state.historyNotes,
                                    onHistoryClick = { noteId ->
                                        onIntent(NoteListIntent.HistoryNoteClicked(noteId = noteId))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Material3 SearchBar overlay
        if (isSearchExpanded) {
            val searchResults = when (searchSelectedTab) {
                0 -> state.filteredMyNotes.map { SearchResultItem(id = it.id, content = it.content) }
                1 -> state.filteredSavedNotes.map { SearchResultItem(id = it.id, content = it.content) }
                2 -> emptyList()
                else -> emptyList()
            }

            NotedSearchBar(
                query = state.searchQuery,
                onQueryChange = { onIntent(NoteListIntent.SearchQueryChanged(it)) },
                onSearch = {
                    isSearchExpanded = false
                },
                onDismiss = {
                    isSearchExpanded = false
                    onIntent(NoteListIntent.SearchQueryChanged(""))
                },
                searchHistory = emptyList(),
                searchSelectedTab = searchSelectedTab,
                onSearchTabSelected = { searchSelectedTab = it },
                searchResults = searchResults,
                onNoteClick = { noteId ->
                    onIntent(NoteListIntent.NoteClicked(noteId = noteId))
                    isSearchExpanded = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .semantics { traversalIndex = 0f },
            )
        }
    }
}

@Composable
private fun NoteListContent(
    notes: List<NoteListItemUi>,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(
            items = notes,
            key = { item -> item.id },
        ) { item ->
            NoteListItem(
                note = item,
                onClick = { onNoteClick(item.id) },
            )
        }
    }
}

@Composable
private fun HistoryListContent(
    historyNotes: List<NoteHistoryItemUi>,
    onHistoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(
            items = historyNotes,
            key = { item -> item.id },
        ) { item ->
            NoteHistoryListItem(
                history = item,
                onClick = { onHistoryClick(item.id) },
            )
        }
    }
}

@Composable
private fun EmptyState(
    selectedTab: Int,
    isSearchActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val message = when {
        isSearchActive -> "Tidak ada note yang cocok dengan pencarian"
        selectedTab == 0 -> "Belum ada note. Tekan tombol + untuk menambahkan note."
        selectedTab == 1 -> "Belum ada note tersimpan. Jelajahi note publik untuk menyimpan."
        selectedTab == 2 -> "Belum ada riwayat. Note yang Anda lihat akan muncul di sini."
        else -> "Tidak ada data"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = errorMessage,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) {
            Text("Coba Lagi")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteListScreenPreviewWithItems() {
    NotedTheme {
        NoteListScreen(
            state = NoteListPreviewData.withItems,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteListScreenPreviewEmpty() {
    NotedTheme {
        NoteListScreen(
            state = NoteListPreviewData.empty,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteListScreenPreviewWithPendingUpload() {
    NotedTheme {
        NoteListScreen(
            state = NoteListPreviewData.withItems.copy(
                syncStatus = id.usecase.noted.data.sync.NoteSyncStatus(
                    pendingUploadCount = 5,
                ),
            ),
            onIntent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NoteListScreenPreviewWithSearchExpanded() {
    NotedTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { isTraversalGroup = true },
        ) {
            // Main screen content
            Column {
                TabRow(
                    selectedTabIndex = 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TAB_TITLES.forEachIndexed { index, title ->
                        Tab(
                            selected = index == 0,
                            onClick = { },
                            text = { Text(title) },
                        )
                    }
                }
                NoteListContent(
                    notes = NoteListPreviewData.withItems.myNotes,
                    onNoteClick = {},
                )
            }

            // Search overlay preview
            NotedSearchBar(
                query = "contoh",
                onQueryChange = { },
                onSearch = { },
                onDismiss = { },
                searchHistory = listOf(
                    "kotlin tutorial",
                    "compose basics",
                    "android development",
                ),
                searchSelectedTab = 0,
                onSearchTabSelected = { },
                searchResults = NoteListPreviewData.withItems.filteredMyNotes.map { 
                    SearchResultItem(id = it.id, content = it.content) 
                },
                onNoteClick = { },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .semantics { traversalIndex = 0f },
            )
        }
    }
}
