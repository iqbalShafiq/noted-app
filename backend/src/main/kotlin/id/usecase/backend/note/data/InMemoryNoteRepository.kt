package id.usecase.backend.note.data

import id.usecase.backend.note.domain.NoteRepository
import id.usecase.backend.note.domain.NoteSyncRepository
import id.usecase.backend.note.domain.NoteVisibility
import id.usecase.backend.note.domain.StoredNote
import id.usecase.backend.note.domain.SyncApplyResult
import id.usecase.backend.note.domain.SyncApplyStatus
import id.usecase.backend.note.domain.SyncPullData
import id.usecase.noted.shared.note.SyncMutationDto
import id.usecase.noted.shared.note.SyncMutationType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryNoteRepository : NoteRepository, NoteSyncRepository {
    private val notesById = linkedMapOf<String, StoredNote>()
    private val syncEvents = mutableListOf<SyncEvent>()
    private val mutex = Mutex()
    private var nextCursor = 1L

    override suspend fun create(note: StoredNote): StoredNote {
        return mutex.withLock {
            notesById[note.id] = note
            recordSyncEvent(
                ownerUserId = note.ownerUserId,
                noteId = note.id,
                operationId = "legacy-create:${note.id}:${note.updatedAtEpochMillis}",
            )
            note
        }
    }

    override suspend fun findById(noteId: String): StoredNote? {
        return mutex.withLock {
            notesById[noteId]
        }
    }

    override suspend fun findByOwner(ownerUserId: String): List<StoredNote> {
        return mutex.withLock {
            notesById.values
                .asSequence()
                .filter { it.ownerUserId == ownerUserId }
                .sortedByDescending { it.createdAtEpochMillis }
                .toList()
        }
    }

    override suspend fun findByIds(noteIds: Set<String>): List<StoredNote> {
        return mutex.withLock {
            notesById.values
                .asSequence()
                .filter { it.id in noteIds }
                .sortedByDescending { it.createdAtEpochMillis }
                .toList()
        }
    }

    override suspend fun findAllExcludingOwner(excludeOwnerUserId: String, limit: Int): List<StoredNote> {
        return mutex.withLock {
            notesById.values
                .asSequence()
                .filter { it.ownerUserId != excludeOwnerUserId && it.deletedAtEpochMillis == null }
                .sortedByDescending { it.createdAtEpochMillis }
                .take(limit)
                .toList()
        }
    }

    override suspend fun findPublicNotes(limit: Int): List<StoredNote> {
        return mutex.withLock {
            notesById.values
                .asSequence()
                .filter { it.visibility == NoteVisibility.PUBLIC && it.deletedAtEpochMillis == null }
                .sortedByDescending { it.createdAtEpochMillis }
                .take(limit)
                .toList()
        }
    }

    override suspend fun applyMutation(
        ownerUserId: String,
        mutation: SyncMutationDto,
        serverNowEpochMillis: Long,
    ): SyncApplyResult {
        return mutex.withLock {
            if (syncEvents.any { it.ownerUserId == ownerUserId && it.operationId == mutation.operationId }) {
                return@withLock SyncApplyResult(
                    status = SyncApplyStatus.DUPLICATE,
                    appliedNote = notesById[mutation.noteId],
                )
            }

            val existing = notesById[mutation.noteId]
            if (existing != null && existing.ownerUserId != ownerUserId) {
                return@withLock SyncApplyResult(
                    status = SyncApplyStatus.CONFLICT,
                    conflictReason = "Note belongs to another owner",
                    conflictServerNote = existing,
                )
            }

            val expectedVersion = mutation.baseVersion ?: existing?.version
            if (existing == null && mutation.baseVersion != null && mutation.baseVersion != 0L) {
                return@withLock SyncApplyResult(
                    status = SyncApplyStatus.CONFLICT,
                    conflictReason = "Note does not exist for baseVersion=${mutation.baseVersion}",
                    conflictServerNote = null,
                )
            }
            if (expectedVersion != null && existing != null && existing.version != expectedVersion) {
                return@withLock SyncApplyResult(
                    status = SyncApplyStatus.CONFLICT,
                    conflictReason = "Version conflict",
                    conflictServerNote = existing,
                )
            }

            val effectiveUpdatedAt = maxOf(serverNowEpochMillis, mutation.clientUpdatedAtEpochMillis)
            val applied = when (mutation.type) {
                SyncMutationType.UPSERT -> {
                    val normalizedContent = mutation.content?.trim().orEmpty()
                    require(normalizedContent.isNotBlank()) { "content must not be blank for UPSERT" }

                    if (existing == null) {
                        StoredNote(
                            id = mutation.noteId,
                            ownerUserId = ownerUserId,
                            content = normalizedContent,
                            createdAtEpochMillis = effectiveUpdatedAt,
                            updatedAtEpochMillis = effectiveUpdatedAt,
                            deletedAtEpochMillis = null,
                            version = 1,
                            visibility = existing?.visibility ?: NoteVisibility.PRIVATE,
                        )
                    } else {
                        existing.copy(
                            content = normalizedContent,
                            updatedAtEpochMillis = effectiveUpdatedAt,
                            deletedAtEpochMillis = null,
                            version = existing.version + 1,
                        )
                    }
                }

                SyncMutationType.DELETE -> {
                    if (existing == null) {
                        StoredNote(
                            id = mutation.noteId,
                            ownerUserId = ownerUserId,
                            content = "",
                            createdAtEpochMillis = effectiveUpdatedAt,
                            updatedAtEpochMillis = effectiveUpdatedAt,
                            deletedAtEpochMillis = effectiveUpdatedAt,
                            version = 1,
                            visibility = NoteVisibility.PRIVATE,
                        )
                    } else {
                        existing.copy(
                            updatedAtEpochMillis = effectiveUpdatedAt,
                            deletedAtEpochMillis = effectiveUpdatedAt,
                            version = existing.version + 1,
                        )
                    }
                }
            }

            notesById[applied.id] = applied
            recordSyncEvent(
                ownerUserId = ownerUserId,
                noteId = applied.id,
                operationId = mutation.operationId,
            )

            SyncApplyResult(
                status = SyncApplyStatus.APPLIED,
                appliedNote = applied,
            )
        }
    }

    override suspend fun pullChanges(ownerUserId: String, afterCursor: Long): SyncPullData {
        return mutex.withLock {
            val changedEvents = syncEvents
                .asSequence()
                .filter { event -> event.ownerUserId == ownerUserId && event.cursor > afterCursor }
                .toList()

            val changedNoteIds = changedEvents
                .asReversed()
                .map { it.noteId }
                .distinct()

            val notes = changedNoteIds.mapNotNull { noteId -> notesById[noteId] }
                .sortedBy { note ->
                    changedEvents.firstOrNull { it.noteId == note.id }?.cursor ?: Long.MAX_VALUE
                }

            val latestCursor = syncEvents
                .asSequence()
                .filter { it.ownerUserId == ownerUserId }
                .maxOfOrNull { it.cursor }
                ?: 0L

            SyncPullData(
                cursor = latestCursor,
                notes = notes,
            )
        }
    }

    override suspend fun currentCursor(ownerUserId: String): Long {
        return mutex.withLock {
            syncEvents
                .asSequence()
                .filter { it.ownerUserId == ownerUserId }
                .maxOfOrNull { it.cursor }
                ?: 0L
        }
    }

    private fun recordSyncEvent(ownerUserId: String, noteId: String, operationId: String) {
        syncEvents += SyncEvent(
            cursor = nextCursor,
            ownerUserId = ownerUserId,
            noteId = noteId,
            operationId = operationId,
        )
        nextCursor += 1
    }
}

private data class SyncEvent(
    val cursor: Long,
    val ownerUserId: String,
    val noteId: String,
    val operationId: String,
)
