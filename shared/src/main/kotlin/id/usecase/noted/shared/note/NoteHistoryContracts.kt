package id.usecase.noted.shared.note

import kotlinx.serialization.Serializable

@Serializable
data class NoteHistoryDto(
    val noteId: String,
    val ownerUserId: String,
    val content: String,
    val viewedAtEpochMillis: Long,
)

@Serializable
data class AddHistoryRequest(
    val noteId: String,
)
