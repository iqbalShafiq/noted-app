package id.usecase.backend.data.note

import id.usecase.backend.domain.note.NoteCommentRepository
import id.usecase.backend.domain.note.StoredNoteComment
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryNoteCommentRepository : NoteCommentRepository {
    private val comments = mutableListOf<StoredNoteComment>()
    private val mutex = Mutex()

    override suspend fun create(comment: StoredNoteComment): StoredNoteComment {
        return mutex.withLock {
            comments += comment
            comment
        }
    }

    override suspend fun countByNoteId(noteId: String): Int {
        return mutex.withLock {
            comments.count { it.noteId == noteId }
        }
    }

    override suspend fun listByNoteId(
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long?,
    ): List<StoredNoteComment> {
        require(limit > 0) { "limit must be positive" }
        return mutex.withLock {
            val sortedComments = comments
                .asSequence()
                .filter { it.noteId == noteId }
                .filter { comment ->
                    beforeEpochMillis == null || comment.createdAtEpochMillis < beforeEpochMillis
                }
                .sortedWith(
                    compareByDescending<StoredNoteComment> { it.createdAtEpochMillis }
                        .thenByDescending { it.id },
                )
                .toList()

            if (sortedComments.size <= limit) {
                return@withLock sortedComments
            }

            val boundaryTimestamp = sortedComments[limit - 1].createdAtEpochMillis
            var lastIndex = limit
            while (lastIndex < sortedComments.size && sortedComments[lastIndex].createdAtEpochMillis == boundaryTimestamp) {
                lastIndex += 1
            }

            sortedComments.subList(0, lastIndex)
        }
    }
}
