package id.usecase.backend.domain.note

data class StoredNoteComment(
    val id: String,
    val noteId: String,
    val authorUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
)
