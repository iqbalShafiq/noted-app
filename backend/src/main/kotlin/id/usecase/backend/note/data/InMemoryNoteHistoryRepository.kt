package id.usecase.backend.note.data

import id.usecase.backend.note.domain.NoteHistoryRepository
import id.usecase.backend.note.domain.StoredNote
import id.usecase.backend.note.domain.StoredNoteHistory
import java.util.UUID

class InMemoryNoteHistoryRepository : NoteHistoryRepository {
    private val history = mutableListOf<StoredNoteHistory>()

    override suspend fun addHistory(userId: String, noteId: String, note: StoredNote) {
        val existingIndex = history.indexOfFirst { it.userId == userId && it.noteId == noteId }
        val now = System.currentTimeMillis()

        if (existingIndex != -1) {
            history[existingIndex] = history[existingIndex].copy(
                content = note.content,
                viewedAtEpochMillis = now
            )
        } else {
            history.add(
                StoredNoteHistory(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    noteId = noteId,
                    noteOwnerId = note.ownerUserId,
                    content = note.content,
                    viewedAtEpochMillis = now
                )
            )
        }
    }

    override suspend fun getHistory(userId: String, limit: Int): List<StoredNoteHistory> {
        return history
            .filter { it.userId == userId }
            .sortedByDescending { it.viewedAtEpochMillis }
            .take(limit)
    }

    override suspend fun clearHistory(userId: String) {
        history.removeAll { it.userId == userId }
    }
}
