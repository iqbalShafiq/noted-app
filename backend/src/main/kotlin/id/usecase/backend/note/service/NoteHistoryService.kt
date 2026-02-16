package id.usecase.backend.note.service

import id.usecase.backend.note.domain.NoteHistoryRepository
import id.usecase.backend.note.domain.NoteRepository
import id.usecase.backend.note.domain.StoredNoteHistory

class NoteHistoryService(
    private val noteHistoryRepository: NoteHistoryRepository,
    private val noteRepository: NoteRepository,
) {
    suspend fun recordHistory(userId: String, noteId: String) {
        val normalizedUserId = userId.trim()
        val normalizedNoteId = noteId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedNoteId.isNotBlank()) { "noteId must not be blank" }

        val note = noteRepository.findById(normalizedNoteId)
            ?.takeIf { it.deletedAtEpochMillis == null }
            ?: throw IllegalArgumentException("Note with id '$normalizedNoteId' not found")

        noteHistoryRepository.addHistory(normalizedUserId, normalizedNoteId, note)
    }

    suspend fun getUserHistory(userId: String, limit: Int): List<StoredNoteHistory> {
        val normalizedUserId = userId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(limit > 0) { "limit must be positive" }

        return noteHistoryRepository.getHistory(normalizedUserId, limit)
    }

    suspend fun clearUserHistory(userId: String) {
        val normalizedUserId = userId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }

        noteHistoryRepository.clearHistory(normalizedUserId)
    }
}
