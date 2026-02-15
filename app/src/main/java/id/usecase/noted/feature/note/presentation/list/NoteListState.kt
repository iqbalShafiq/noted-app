package id.usecase.noted.feature.note.presentation.list

data class NoteListState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val notes: List<NoteListItemUi> = emptyList(),
)

data class NoteListItemUi(
    val id: Long,
    val content: String,
    val createdAt: Long,
)
