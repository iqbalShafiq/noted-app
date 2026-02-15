package id.usecase.noted.shared.note

import kotlinx.serialization.Serializable

@Serializable
data class CreateNoteRequest(
    val ownerUserId: String,
    val content: String,
)

@Serializable
data class ShareNoteRequest(
    val recipientUserId: String,
)

@Serializable
data class NoteDto(
    val id: String,
    val ownerUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val sharedWithUserIds: List<String> = emptyList(),
)

@Serializable
data class ShareNoteResponse(
    val noteId: String,
    val recipientUserId: String,
    val sharedAtEpochMillis: Long,
)
