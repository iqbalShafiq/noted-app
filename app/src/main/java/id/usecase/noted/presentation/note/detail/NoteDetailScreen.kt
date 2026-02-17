package id.usecase.noted.presentation.note.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.filled.ForkLeft
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.usecase.noted.presentation.components.content.InfoRow
import id.usecase.noted.presentation.components.feedback.ErrorState
import id.usecase.noted.presentation.components.feedback.LoadingState
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
import id.usecase.noted.ui.theme.NotedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteDetailScreenRoot(
    viewModel: NoteDetailViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NoteDetailEffect.NavigateBack -> onNavigateBack()
                is NoteDetailEffect.NavigateToEditor -> onNavigateToEditor(effect.localNoteId)
                is NoteDetailEffect.ShowMessage -> onShowMessage(effect.message)
            }
        }
    }

    NoteDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    state: NoteDetailState,
    onIntent: (NoteDetailIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canPerformActions = state.note != null && !state.isLoading

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NotedTopAppBar(
                title = "Detail Catatan",
                onNavigateBack = onNavigateBack,
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        onClick = { onIntent(NoteDetailIntent.CopyContentClicked) },
                        enabled = canPerformActions,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Salin konten",
                        )
                    }
                    IconButton(
                        onClick = { onIntent(NoteDetailIntent.SaveClicked) },
                        enabled = canPerformActions,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Simpan note",
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = BottomAppBarDefaults.ContainerElevation,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = { Text("Fork") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.ForkLeft,
                                contentDescription = null,
                            )
                        },
                        onClick = { onIntent(NoteDetailIntent.ForkClicked) },
                        expanded = canPerformActions,
                        containerColor = if (canPerformActions) {
                            BottomAppBarDefaults.bottomAppBarFabColor
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        elevation = if (canPerformActions) {
                            FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        } else {
                            FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                        },
                    )
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
                state.isLoading && state.note == null -> {
                    LoadingState()
                }

                state.errorMessage != null -> {
                    ErrorState(
                        message = state.errorMessage,
                        onRetry = { onIntent(NoteDetailIntent.RetryClicked) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                state.note != null -> {
                    NoteContent(
                        note = state.note,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteContent(
    note: ExternalNote,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val formattedDate = remember(note.createdAt) {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.createdAt))
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Informasi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                InfoRow(label = "Pemilik", value = "@${note.ownerUserId.take(8)}...")
                InfoRow(label = "Dibuat", value = formattedDate)

                if (note.forkedFrom != null) {
                    InfoRow(
                        label = "Fork dari",
                        value = "Note ${note.forkedFrom.take(8)}...",
                    )
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun NoteDetailScreenPreview() {
    NotedTheme {
        NoteDetailScreen(
            state = NoteDetailState(
                isLoading = false,
                note = ExternalNote(
                    id = "note123",
                    ownerUserId = "user456",
                    content = "Ini adalah konten note yang dibagikan oleh pengguna lain. Note ini berisi informasi penting yang bisa di-fork atau disimpan.",
                    createdAt = System.currentTimeMillis(),
                    forkedFrom = null,
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteDetailScreenForkedPreview() {
    NotedTheme {
        NoteDetailScreen(
            state = NoteDetailState(
                isLoading = false,
                note = ExternalNote(
                    id = "note123",
                    ownerUserId = "user456",
                    content = "Note ini telah di-fork dari note lain.",
                    createdAt = System.currentTimeMillis(),
                    forkedFrom = "original789",
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteDetailScreenLoadingPreview() {
    NotedTheme {
        NoteDetailScreen(
            state = NoteDetailState(isLoading = true),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteDetailScreenErrorPreview() {
    NotedTheme {
        NoteDetailScreen(
            state = NoteDetailState(
                isLoading = false,
                errorMessage = "Gagal memuat note. Cek koneksi internet.",
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
