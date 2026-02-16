package id.usecase.noted.domain

data class NoteHistory(
    val id: Long = 0,
    val noteId: String,
    val ownerUserId: String,
    val content: String,
    val viewedAt: Long,
)
