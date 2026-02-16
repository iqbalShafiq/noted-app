package id.usecase.noted.shared.note

import kotlinx.serialization.Serializable

@Serializable
data class SyncPushRequest(
    val deviceId: String,
    val mutations: List<SyncMutationDto>,
)

@Serializable
data class SyncMutationDto(
    val operationId: String,
    val noteId: String,
    val type: SyncMutationType,
    val content: String? = null,
    val clientUpdatedAtEpochMillis: Long,
    val baseVersion: Long? = null,
)

@Serializable
enum class SyncMutationType {
    UPSERT,
    DELETE,
}

@Serializable
data class SyncPushResponse(
    val acceptedOperationIds: List<String>,
    val conflicts: List<SyncConflictDto>,
    val appliedNotes: List<SyncedNoteDto>,
    val cursor: Long,
)

@Serializable
data class SyncConflictDto(
    val operationId: String,
    val noteId: String,
    val reason: String,
    val serverNote: SyncedNoteDto? = null,
)

@Serializable
data class SyncPullResponse(
    val userId: String,
    val cursor: Long,
    val notes: List<SyncedNoteDto>,
)

@Serializable
data class SyncedNoteDto(
    val noteId: String,
    val ownerUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val version: Long,
    val sharedWithUserIds: List<String> = emptyList(),
    val visibility: String = "PRIVATE",
)
