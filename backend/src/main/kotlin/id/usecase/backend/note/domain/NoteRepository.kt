package id.usecase.backend.note.domain

import id.usecase.noted.shared.note.SyncMutationDto

data class StoredNote(
    val id: String,
    val ownerUserId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val version: Long,
)

interface NoteRepository {
    suspend fun create(note: StoredNote): StoredNote

    suspend fun findById(noteId: String): StoredNote?

    suspend fun findByOwner(ownerUserId: String): List<StoredNote>

    suspend fun findByIds(noteIds: Set<String>): List<StoredNote>
}

interface NoteSyncRepository {
    suspend fun applyMutation(
        ownerUserId: String,
        mutation: SyncMutationDto,
        serverNowEpochMillis: Long,
    ): SyncApplyResult

    suspend fun pullChanges(ownerUserId: String, afterCursor: Long): SyncPullData

    suspend fun currentCursor(ownerUserId: String): Long
}

data class SyncPullData(
    val cursor: Long,
    val notes: List<StoredNote>,
)

data class SyncApplyResult(
    val status: SyncApplyStatus,
    val appliedNote: StoredNote? = null,
    val conflictReason: String? = null,
    val conflictServerNote: StoredNote? = null,
)

enum class SyncApplyStatus {
    APPLIED,
    DUPLICATE,
    CONFLICT,
}
