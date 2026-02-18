package id.usecase.backend.service.note

import id.usecase.backend.domain.note.NoteCommentRepository
import id.usecase.backend.domain.note.NoteRepository
import id.usecase.backend.domain.note.NoteShareRepository
import id.usecase.backend.domain.note.NoteVisibility
import id.usecase.backend.domain.note.NoteReactionRepository
import id.usecase.backend.domain.note.StoredNoteComment
import id.usecase.backend.domain.note.StoredNoteEngagement
import java.util.UUID

data class StoredNoteCommentsPage(
    val items: List<StoredNoteComment>,
    val nextBeforeEpochMillis: Long?,
)

class NoteEngagementService(
    private val noteRepository: NoteRepository,
    private val noteShareRepository: NoteShareRepository,
    private val noteCommentRepository: NoteCommentRepository,
    private val noteReactionRepository: NoteReactionRepository,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun getEngagement(userId: String, noteId: String): StoredNoteEngagement {
        val normalizedUserId = userId.trim()
        val normalizedNoteId = noteId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedNoteId.isNotBlank()) { "noteId must not be blank" }

        ensureAccessible(userId = normalizedUserId, noteId = normalizedNoteId)
        return buildEngagement(userId = normalizedUserId, noteId = normalizedNoteId)
    }

    suspend fun addLove(userId: String, noteId: String): StoredNoteEngagement {
        val normalizedUserId = userId.trim()
        val normalizedNoteId = noteId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedNoteId.isNotBlank()) { "noteId must not be blank" }

        ensureAccessible(userId = normalizedUserId, noteId = normalizedNoteId)
        noteReactionRepository.addLove(noteId = normalizedNoteId, userId = normalizedUserId)
        return buildEngagement(userId = normalizedUserId, noteId = normalizedNoteId)
    }

    suspend fun removeLove(userId: String, noteId: String): StoredNoteEngagement {
        val normalizedUserId = userId.trim()
        val normalizedNoteId = noteId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedNoteId.isNotBlank()) { "noteId must not be blank" }

        ensureAccessible(userId = normalizedUserId, noteId = normalizedNoteId)
        noteReactionRepository.removeLove(noteId = normalizedNoteId, userId = normalizedUserId)
        return buildEngagement(userId = normalizedUserId, noteId = normalizedNoteId)
    }

    suspend fun listComments(
        userId: String,
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long?,
    ): StoredNoteCommentsPage {
        val normalizedUserId = userId.trim()
        val normalizedNoteId = noteId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedNoteId.isNotBlank()) { "noteId must not be blank" }
        require(limit > 0) { "limit must be positive" }

        ensureAccessible(userId = normalizedUserId, noteId = normalizedNoteId)
        val effectiveLimit = limit.coerceAtMost(MAX_COMMENT_PAGE_SIZE)
        val items = noteCommentRepository.listByNoteId(
            noteId = normalizedNoteId,
            limit = effectiveLimit,
            beforeEpochMillis = beforeEpochMillis,
        )
        val nextBeforeEpochMillis = items.lastOrNull()
            ?.createdAtEpochMillis
            ?.takeIf { boundaryEpochMillis ->
                noteCommentRepository.listByNoteId(
                    noteId = normalizedNoteId,
                    limit = 1,
                    beforeEpochMillis = boundaryEpochMillis,
                ).isNotEmpty()
            }
        return StoredNoteCommentsPage(
            items = items,
            nextBeforeEpochMillis = nextBeforeEpochMillis,
        )
    }

    suspend fun addComment(userId: String, noteId: String, content: String): StoredNoteComment {
        val normalizedUserId = userId.trim()
        val normalizedNoteId = noteId.trim()
        val normalizedContent = content.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedNoteId.isNotBlank()) { "noteId must not be blank" }
        require(content.any { !it.isWhitespace() }) { "content must contain at least one non-whitespace character" }
        require(content.length <= MAX_COMMENT_CONTENT_LENGTH) {
            "content must be at most $MAX_COMMENT_CONTENT_LENGTH characters"
        }

        ensureAccessible(userId = normalizedUserId, noteId = normalizedNoteId)
        return noteCommentRepository.create(
            StoredNoteComment(
                id = idGenerator(),
                noteId = normalizedNoteId,
                authorUserId = normalizedUserId,
                content = normalizedContent,
                createdAtEpochMillis = clock(),
            ),
        )
    }

    private suspend fun buildEngagement(userId: String, noteId: String): StoredNoteEngagement {
        val loveSnapshot = noteReactionRepository.getLoveSnapshot(noteId = noteId, userId = userId)
        return StoredNoteEngagement(
            noteId = noteId,
            loveCount = loveSnapshot.loveCount,
            hasLovedByMe = loveSnapshot.hasLovedByMe,
            commentCount = noteCommentRepository.countByNoteId(noteId),
        )
    }

    private suspend fun ensureAccessible(userId: String, noteId: String) {
        val note = noteRepository.findById(noteId)
            ?.takeIf { it.deletedAtEpochMillis == null }
            ?: throw IllegalArgumentException("Note with id '$noteId' not found")

        val hasAccess = note.ownerUserId == userId ||
            note.visibility == NoteVisibility.PUBLIC ||
            note.visibility == NoteVisibility.LINK_SHARED ||
            noteShareRepository.hasShare(noteId = noteId, recipientUserId = userId)

        if (!hasAccess) {
            throw IllegalArgumentException("Note with id '$noteId' is not accessible")
        }
    }

    private companion object {
        const val MAX_COMMENT_CONTENT_LENGTH = 500
        const val MAX_COMMENT_PAGE_SIZE = 100
    }
}
