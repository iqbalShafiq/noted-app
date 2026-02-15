package id.usecase.noted.presentation.note.explore

data class ExploreState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val notes: List<ExploreNoteUi> = emptyList(),
)

data class ExploreNoteUi(
    val id: String,
    val content: String,
    val ownerUserId: String,
    val createdAt: Long,
)
