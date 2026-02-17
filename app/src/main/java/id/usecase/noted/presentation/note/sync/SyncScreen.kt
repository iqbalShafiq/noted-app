package id.usecase.noted.presentation.note.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import id.usecase.noted.presentation.components.navigation.NotedBottomAppBar
import id.usecase.noted.presentation.components.navigation.NotedFloatingActionButton
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
import id.usecase.noted.ui.theme.NotedTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun SyncScreenRoot(
    viewModel: SyncViewModel,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SyncEffect.NavigateBack -> onNavigateBack()
                SyncEffect.NavigateToAccount -> onNavigateToAccount()
                SyncEffect.NavigateToLogin -> onNavigateToLogin()
                is SyncEffect.ShowMessage -> onShowMessage(effect.message)
            }
        }
    }

    SyncScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun SyncScreen(
    state: SyncState,
    onIntent: (SyncIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NotedTopAppBar(
                title = "Sinkronisasi",
                onNavigateBack = { onIntent(SyncIntent.NavigateBackClicked) },
            )
        },
        bottomBar = {
            SyncBottomBar(
                isLoggedIn = state.syncStatus.isLoggedIn,
                onUploadClicked = { onIntent(SyncIntent.UploadClicked) },
                onAccountClicked = { onIntent(SyncIntent.AccountClicked) },
                onLoginClicked = { onIntent(SyncIntent.LoginClicked) },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SyncStatusCard(
                    syncStatus = state.syncStatus,
                )

                if (state.syncStatus.isSyncing) {
                    ProgressSection(
                        uploadedCount = state.syncStatus.uploadedCount,
                        totalToUpload = state.syncStatus.totalToUpload,
                    )
                }

                if (!state.syncStatus.isLoggedIn) {
                    Text(
                        text = "Login diperlukan untuk sinkronisasi cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.pendingNotes.isNotEmpty()) {
                    Text(
                        text = "Pending Upload (${state.pendingNotes.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    PendingNotesList(
                        pendingNotes = state.pendingNotes,
                        onRetryClicked = { noteId ->
                            onIntent(SyncIntent.RetryNoteClicked(noteId))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncBottomBar(
    isLoggedIn: Boolean,
    onUploadClicked: () -> Unit,
    onAccountClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NotedBottomAppBar(
        modifier = modifier,
        actions = {
            IconButton(
                onClick = if (isLoggedIn) onAccountClicked else onLoginClicked,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = if (isLoggedIn) "Akun" else "Login",
                )
            }
        },
        floatingActionButton = {
            NotedFloatingActionButton(onClick = onUploadClicked) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Upload",
                )
            }
        },
    )
}

@Composable
private fun SyncStatusCard(
    syncStatus: NoteSyncStatus,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Status Koneksi",
                    style = MaterialTheme.typography.titleSmall,
                )

                ConnectionIndicator(isOnline = syncStatus.isOnline)
            }

            HorizontalDivider()

            if (syncStatus.isLoggedIn) {
                Text(
                    text = "User: ${syncStatus.username ?: syncStatus.userId}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Belum login",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Pending upload: ${syncStatus.pendingUploadCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            syncStatus.lastSyncAtEpochMillis?.let { timestamp ->
                Text(
                    text = "Sinkron terakhir: ${DateFormat.getDateTimeInstance().format(Date(timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            syncStatus.lastEventMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            syncStatus.lastErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ConnectionIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .then(
                    if (isOnline) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50),
                        )
                    } else {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(50),
                        )
                    }
                ),
        )

        Text(
            text = if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOnline) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun ProgressSection(
    uploadedCount: Int,
    totalToUpload: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Sedang Sinkronisasi...",
                style = MaterialTheme.typography.titleSmall,
            )

            val progress = if (totalToUpload <= 0) {
                0f
            } else {
                uploadedCount.toFloat() / totalToUpload.toFloat()
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Upload $uploadedCount/$totalToUpload",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PendingNotesList(
    pendingNotes: List<PendingNoteUi>,
    onRetryClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = pendingNotes,
            key = { item -> item.id },
        ) { note ->
            PendingNoteItem(
                note = note,
                onRetryClicked = { onRetryClicked(note.id) },
            )
        }
    }
}

@Composable
private fun PendingNoteItem(
    note: PendingNoteUi,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isFailed) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = DateFormat.getDateTimeInstance().format(Date(note.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (note.isFailed) {
                    IconButton(onClick = onRetryClicked) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (note.isFailed && note.errorMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )

                    Text(
                        text = note.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncScreenPreview() {
    NotedTheme {
        SyncScreen(
            state = SyncState(
                syncStatus = NoteSyncStatus(
                    isOnline = true,
                    userId = "user123",
                    username = "testuser",
                    isSyncing = false,
                    pendingUploadCount = 2,
                    lastSyncAtEpochMillis = System.currentTimeMillis(),
                ),
                pendingNotes = listOf(
                    PendingNoteUi(
                        id = 1,
                        content = "Note pending pertama",
                        createdAt = System.currentTimeMillis() - 3600000,
                        isFailed = false,
                    ),
                    PendingNoteUi(
                        id = 2,
                        content = "Note yang gagal sync",
                        createdAt = System.currentTimeMillis() - 7200000,
                        isFailed = true,
                        errorMessage = "Conflict dengan server",
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncScreenSyncingPreview() {
    NotedTheme {
        SyncScreen(
            state = SyncState(
                syncStatus = NoteSyncStatus(
                    isOnline = true,
                    userId = "user123",
                    username = "testuser",
                    isSyncing = true,
                    pendingUploadCount = 3,
                    uploadedCount = 2,
                    totalToUpload = 5,
                    lastEventMessage = "Upload 2/5",
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncScreenOfflinePreview() {
    NotedTheme {
        SyncScreen(
            state = SyncState(
                syncStatus = NoteSyncStatus(
                    isOnline = false,
                    isSyncing = false,
                    pendingUploadCount = 0,
                ),
            ),
            onIntent = {},
        )
    }
}
