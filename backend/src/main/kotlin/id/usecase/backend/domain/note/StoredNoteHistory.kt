package id.usecase.backend.note.domain

data class StoredNoteHistory(
    val id: String,
    val userId: String,
    val noteId: String,
    val noteOwnerId: String,
    val content: String,
    val viewedAtEpochMillis: Long,
)
