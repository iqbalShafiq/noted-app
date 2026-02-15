package id.usecase.noted.feature.note.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.usecase.noted.feature.note.presentation.list.component.NoteListItem
import id.usecase.noted.feature.note.presentation.list.preview.NoteListPreviewData
import id.usecase.noted.ui.theme.NotedTheme

@Composable
fun NoteListScreenRoot(
    viewModel: NoteListViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NoteListEffect.NavigateToEditor -> onNavigateToEditor(effect.noteId)
                is NoteListEffect.ShowMessage -> onShowMessage(effect.message)
            }
        }
    }

    NoteListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun NoteListScreen(
    state: NoteListState,
    onIntent: (NoteListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.errorMessage,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = { onIntent(NoteListIntent.RetryObserve) },
                    ) {
                        Text("Coba Lagi")
                    }
                }
            }

            state.notes.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Belum ada note. Tekan tombol + untuk menambahkan note.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.notes,
                        key = { item -> item.id },
                    ) { item ->
                        NoteListItem(
                            note = item,
                            onClick = {
                                onIntent(NoteListIntent.NoteClicked(noteId = item.id))
                            },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onIntent(NoteListIntent.AddNoteClicked) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Note",
            )
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
