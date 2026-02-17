package id.usecase.backend.domain.note

interface NoteHistoryRepository {
    suspend fun addHistory(userId: String, noteId: String, note: StoredNote)
    suspend fun getHistory(userId: String, limit: Int = 50): List<StoredNoteHistory>
    suspend fun clearHistory(userId: String)
}
