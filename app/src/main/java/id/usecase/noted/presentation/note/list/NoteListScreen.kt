package id.usecase.noted.presentation.note.list

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import id.usecase.noted.data.sync.NoteSyncStatus
import id.usecase.noted.presentation.note.list.component.NoteListItem
import id.usecase.noted.presentation.note.list.preview.NoteListPreviewData
import id.usecase.noted.ui.theme.NotedTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun NoteListScreenRoot(
    viewModel: NoteListViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NoteListEffect.NavigateToEditor -> onNavigateToEditor(effect.noteId)
                NoteListEffect.NavigateToAuth -> onNavigateToAuth()
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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SyncStatusCard(
                syncStatus = state.syncStatus,
                onAuthClicked = { onIntent(NoteListIntent.AuthClicked) },
                onSyncClicked = { onIntent(NoteListIntent.SyncNowClicked) },
                onUploadClicked = { onIntent(NoteListIntent.UploadNowClicked) },
                onImportClicked = { onIntent(NoteListIntent.ImportNowClicked) },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize(),
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
                                    onDeleteClick = {
                                        onIntent(NoteListIntent.NoteDeleteClicked(noteId = item.id))
                                    },
                                )
                            }
                        }
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

@Composable
private fun SyncStatusCard(
    syncStatus: NoteSyncStatus,
    onAuthClicked: () -> Unit,
    onSyncClicked: () -> Unit,
    onUploadClicked: () -> Unit,
    onImportClicked: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (syncStatus.isLoggedIn) {
                    "Akun: ${syncStatus.username ?: syncStatus.userId}"
                } else {
                    "Akun: belum login"
                },
                style = MaterialTheme.typography.titleSmall,
            )

            val connectionText = if (syncStatus.isOnline) "Online" else "Offline"
            Text(
                text = "Status koneksi: $connectionText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (syncStatus.isSyncing) {
                val progress = if (syncStatus.totalToUpload <= 0) {
                    0f
                } else {
                    syncStatus.uploadedCount.toFloat() / syncStatus.totalToUpload.toFloat()
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Upload ${syncStatus.uploadedCount}/${syncStatus.totalToUpload}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Pending upload: ${syncStatus.pendingUploadCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            syncStatus.lastSyncAtEpochMillis?.let { timestamp ->
                Text(
                    text = "Sinkron terakhir: ${DateFormat.getDateTimeInstance().format(Date(timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            syncStatus.lastErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (!syncStatus.isLoggedIn) {
                Text(
                    text = "Masuk ke akun untuk sinkronisasi cloud, registrasi, atau reset password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onAuthClicked,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Buka Login / Register")
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onSyncClicked,
                        enabled = !syncStatus.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sync Sekarang")
                    }
                    OutlinedButton(
                        onClick = onUploadClicked,
                        enabled = !syncStatus.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Upload Manual ke Server")
                    }
                    OutlinedButton(
                        onClick = onImportClicked,
                        enabled = !syncStatus.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Import dari Server")
                    }
                    OutlinedButton(
                        onClick = onAuthClicked,
                        enabled = !syncStatus.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Kelola Akun (Login/Logout)")
                    }
                }
            }
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
