package id.usecase.backend.sync.service

import id.usecase.backend.note.domain.NoteSyncRepository
import id.usecase.backend.note.domain.StoredNote
import id.usecase.backend.note.domain.SyncApplyStatus
import id.usecase.noted.shared.note.SyncConflictDto
import id.usecase.noted.shared.note.SyncMutationDto
import id.usecase.noted.shared.note.SyncMutationType
import id.usecase.noted.shared.note.SyncPullResponse
import id.usecase.noted.shared.note.SyncPushRequest
import id.usecase.noted.shared.note.SyncPushResponse
import id.usecase.noted.shared.note.SyncedNoteDto
import java.sql.SQLTransientException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class NoteSyncService(
    private val noteSyncRepository: NoteSyncRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val logger = LoggerFactory.getLogger(NoteSyncService::class.java)

    suspend fun push(request: SyncPushRequest, userId: String): SyncPushResponse {
        val normalizedUserId = userId.trim()
        val deviceId = request.deviceId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(deviceId.isNotBlank()) { "deviceId must not be blank" }
        require(request.mutations.size <= 500) { "mutations must not exceed 500 per request" }

        val acceptedOperationIds = mutableListOf<String>()
        val conflicts = mutableListOf<SyncConflictDto>()
        val appliedNotesById = linkedMapOf<String, StoredNote>()

        logger.info(
            "sync.push start userId={} deviceId={} mutationCount={}",
            normalizedUserId,
            deviceId,
            request.mutations.size,
        )

        request.mutations.forEach { mutation ->
            validateMutation(mutation)
            val result = executeWithRetry(
                operationName = "applyMutation:${mutation.operationId}",
            ) {
                noteSyncRepository.applyMutation(
                    ownerUserId = normalizedUserId,
                    mutation = mutation,
                    serverNowEpochMillis = clock(),
                )
            }

            when (result.status) {
                SyncApplyStatus.APPLIED -> {
                    acceptedOperationIds += mutation.operationId
                    val applied = result.appliedNote
                    if (applied != null) {
                        appliedNotesById[applied.id] = applied
                    }
                }

                SyncApplyStatus.DUPLICATE -> {
                    acceptedOperationIds += mutation.operationId
                    val applied = result.appliedNote
                    if (applied != null) {
                        appliedNotesById[applied.id] = applied
                    }
                }

                SyncApplyStatus.CONFLICT -> {
                    logger.warn(
                        "sync.push conflict userId={} noteId={} operationId={} reason={}",
                        normalizedUserId,
                        mutation.noteId,
                        mutation.operationId,
                        result.conflictReason,
                    )
                    conflicts += SyncConflictDto(
                        operationId = mutation.operationId,
                        noteId = mutation.noteId,
                        reason = result.conflictReason ?: "Conflict",
                        serverNote = result.conflictServerNote?.toSyncedDto(),
                    )
                }
            }
        }

        val response = SyncPushResponse(
            acceptedOperationIds = acceptedOperationIds,
            conflicts = conflicts,
            appliedNotes = appliedNotesById.values.map { it.toSyncedDto() },
            cursor = noteSyncRepository.currentCursor(normalizedUserId),
        )

        logger.info(
            "sync.push done userId={} accepted={} conflicts={} cursor={}",
            normalizedUserId,
            response.acceptedOperationIds.size,
            response.conflicts.size,
            response.cursor,
        )
        return response
    }

    suspend fun pull(userId: String, cursor: Long?): SyncPullResponse {
        val normalizedUserId = userId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }

        val normalizedCursor = cursor ?: 0L
        require(normalizedCursor >= 0L) { "cursor must be greater than or equal to 0" }

        val pullData = executeWithRetry(operationName = "pullChanges") {
            noteSyncRepository.pullChanges(
                ownerUserId = normalizedUserId,
                afterCursor = normalizedCursor,
            )
        }

        val response = SyncPullResponse(
            userId = normalizedUserId,
            cursor = pullData.cursor,
            notes = pullData.notes.map { it.toSyncedDto() },
        )

        logger.info(
            "sync.pull done userId={} fromCursor={} toCursor={} itemCount={}",
            normalizedUserId,
            normalizedCursor,
            response.cursor,
            response.notes.size,
        )
        return response
    }

    private fun validateMutation(mutation: SyncMutationDto) {
        require(mutation.operationId.isNotBlank()) { "operationId must not be blank" }
        require(mutation.noteId.isNotBlank()) { "noteId must not be blank" }
        require(mutation.clientUpdatedAtEpochMillis > 0L) { "clientUpdatedAtEpochMillis must be positive" }
        if (mutation.type == SyncMutationType.UPSERT) {
            require(!mutation.content.isNullOrBlank()) { "content is required for UPSERT" }
        }
    }

    private suspend fun <T> executeWithRetry(
        operationName: String,
        maxAttempts: Int = 3,
        initialDelayMillis: Long = 150,
        maxDelayMillis: Long = 1_500,
        block: suspend () -> T,
    ): T {
        var attempt = 0
        var delayMillis = initialDelayMillis
        var lastError: Throwable? = null

        while (attempt < maxAttempts) {
            attempt += 1
            try {
                return block()
            } catch (error: Throwable) {
                lastError = error
                val retryable = isRetryable(error)
                if (!retryable || attempt >= maxAttempts) {
                    throw error
                }

                logger.warn(
                    "retry operation={} attempt={} reason={}",
                    operationName,
                    attempt,
                    error.message,
                )
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(maxDelayMillis)
            }
        }

        throw (lastError ?: IllegalStateException("Retry failed without throwable"))
    }

    private fun isRetryable(error: Throwable): Boolean {
        return when (error) {
            is SQLTransientException -> true
            is org.postgresql.util.PSQLException -> {
                error.sqlState == "40001" || error.sqlState == "40P01"
            }

            else -> false
        }
    }
}

private fun StoredNote.toSyncedDto(): SyncedNoteDto {
    return SyncedNoteDto(
        noteId = id,
        ownerUserId = ownerUserId,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        version = version,
    )
}
