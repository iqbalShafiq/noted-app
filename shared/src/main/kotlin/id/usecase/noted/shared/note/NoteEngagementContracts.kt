package id.usecase.noted.shared.note

import kotlinx.serialization.Serializable

@Serializable
enum class NoteReactionType {
    LOVE,
}

@Serializable
data class NoteEngagementDto(
    val noteId: String,
    val loveCount: Int,
    val hasLovedByMe: Boolean,
    val commentCount: Int,
)

@Serializable
data class NoteCommentDto(
    val id: String,
    val noteId: String,
    val authorUserId: String,
    val authorUsername: String,
    val content: String,
    val createdAtEpochMillis: Long,
)

@Serializable
data class NoteCommentsPageDto(
    val items: List<NoteCommentDto>,
    val nextBeforeEpochMillis: Long? = null,
)

@Serializable
data class CreateNoteCommentRequest(
    val content: String,
)
