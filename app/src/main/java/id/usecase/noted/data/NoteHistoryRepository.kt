package id.usecase.noted.data

import id.usecase.noted.domain.NoteHistory
import kotlinx.coroutines.flow.Flow

interface NoteHistoryRepository {
    suspend fun addToHistory(noteId: String, ownerUserId: String, content: String)

    fun getHistory(limit: Int = 50): Flow<List<NoteHistory>>

    suspend fun clearHistory()

    suspend fun syncHistoryWithServer()
}
