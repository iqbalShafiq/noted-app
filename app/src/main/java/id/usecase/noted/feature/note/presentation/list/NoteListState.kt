package id.usecase.noted.feature.note.presentation.list

import id.usecase.noted.feature.note.data.sync.NoteSyncStatus

data class NoteListState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val notes: List<NoteListItemUi> = emptyList(),
    val syncStatus: NoteSyncStatus = NoteSyncStatus(),
)

data class NoteListItemUi(
    val id: Long,
    val content: String,
    val createdAt: Long,
)
