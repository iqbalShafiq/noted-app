package id.usecase.noted.presentation.note.sync

import id.usecase.noted.data.sync.NoteSyncStatus

data class SyncState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val syncStatus: NoteSyncStatus = NoteSyncStatus(),
    val pendingNotes: List<PendingNoteUi> = emptyList(),
)

data class PendingNoteUi(
    val id: Long,
    val content: String,
    val createdAt: Long,
    val isFailed: Boolean,
    val errorMessage: String? = null,
)
