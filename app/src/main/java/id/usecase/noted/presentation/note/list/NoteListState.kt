package id.usecase.noted.presentation.note.list

import id.usecase.noted.data.sync.NoteSyncStatus
import id.usecase.noted.domain.NoteVisibility

data class NoteListState(
    val selectedTab: Int = 0,
    val myNotes: List<NoteListItemUi> = emptyList(),
    val savedNotes: List<NoteListItemUi> = emptyList(),
    val historyNotes: List<NoteHistoryItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val syncStatus: NoteSyncStatus = NoteSyncStatus(),
    val searchQuery: String = "",
) {
    val filteredMyNotes: List<NoteListItemUi>
        get() = if (searchQuery.isBlank()) {
            myNotes
        } else {
            myNotes.filter { note ->
                note.content.contains(searchQuery, ignoreCase = true)
            }
        }

    val filteredSavedNotes: List<NoteListItemUi>
        get() = if (searchQuery.isBlank()) {
            savedNotes
        } else {
            savedNotes.filter { note ->
                note.content.contains(searchQuery, ignoreCase = true)
            }
        }

    val currentTabItems: List<NoteListItemUi>
        get() = when (selectedTab) {
            0 -> filteredMyNotes
            1 -> filteredSavedNotes
            else -> emptyList()
        }

    val isCurrentTabEmpty: Boolean
        get() = when (selectedTab) {
            0 -> filteredMyNotes.isEmpty()
            1 -> filteredSavedNotes.isEmpty()
            2 -> historyNotes.isEmpty()
            else -> true
        }
}

data class NoteListItemUi(
    val id: Long,
    val content: String,
    val createdAt: Long,
    val visibility: NoteVisibility = NoteVisibility.PRIVATE,
)

data class NoteHistoryItemUi(
    val id: String,
    val ownerUserId: String,
    val title: String,
    val preview: String,
    val viewedAt: Long,
)
