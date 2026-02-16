package id.usecase.noted.presentation.note.detail

data class NoteDetailState(
    val isLoading: Boolean = false,
    val note: ExternalNote? = null,
    val errorMessage: String? = null,
)

data class ExternalNote(
    val id: String,
    val ownerUserId: String,
    val content: String,
    val createdAt: Long,
    val forkedFrom: String?,
)
