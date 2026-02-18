package id.usecase.backend.domain.note

data class StoredNoteEngagement(
    val noteId: String,
    val loveCount: Int,
    val hasLovedByMe: Boolean,
    val commentCount: Int,
)
