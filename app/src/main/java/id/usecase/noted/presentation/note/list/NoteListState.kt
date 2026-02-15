package id.usecase.noted.presentation.note.list

import id.usecase.noted.data.sync.NoteSyncStatus

data class NoteListState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val notes: List<NoteListItemUi> = emptyList(),
    val syncStatus: NoteSyncStatus = NoteSyncStatus(),
    val searchQuery: String = "",
) {
    val filteredNotes: List<NoteListItemUi>
        get() = if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                note.content.contains(searchQuery, ignoreCase = true)
            }
        }
}

data class NoteListItemUi(
    val id: Long,
    val content: String,
    val createdAt: Long,
)
