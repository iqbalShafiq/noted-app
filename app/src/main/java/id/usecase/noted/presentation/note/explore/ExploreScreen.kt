package id.usecase.noted.presentation.note.explore

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.usecase.noted.ui.theme.NotedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExploreScreenRoot(
    viewModel: ExploreViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ExploreEffect.NavigateBack -> onNavigateBack()
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Jelajahi") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ExploreIntent.NavigateBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
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
                            onClick = { onIntent(ExploreIntent.LoadExploreNotes) },
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
                            text = "Belum ada note dari pengguna lain",
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
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(
                            items = state.notes,
                            key = { item -> item.id },
                        ) { item ->
                            ExploreNoteItem(note = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreNoteItem(
    note: ExploreNoteUi,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(note.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.createdAt))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
