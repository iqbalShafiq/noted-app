package id.usecase.noted.presentation.note.explore

data class ExploreState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val notes: List<ExploreNoteUi> = emptyList(),
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val searchHistory: List<String> = emptyList(),
) {
    val filteredNotes: List<ExploreNoteUi>
        get() = if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                note.content.contains(searchQuery, ignoreCase = true)
            }
        }
}

data class ExploreNoteUi(
    val id: String,
    val content: String,
    val ownerUserId: String,
    val createdAt: Long,
)
