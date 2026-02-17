package id.usecase.noted.data

import id.usecase.noted.data.local.NoteHistoryDao
import id.usecase.noted.data.local.NoteHistoryEntity
import id.usecase.noted.data.sync.NoteHistoryApi
import id.usecase.noted.data.sync.SessionStore
import id.usecase.noted.domain.NoteHistory
import id.usecase.noted.domain.NoteHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteHistoryRepositoryImpl(
    private val noteHistoryDao: NoteHistoryDao,
    private val noteHistoryApi: NoteHistoryApi,
    private val sessionStore: SessionStore,
) : NoteHistoryRepository {

    override suspend fun addToHistory(noteId: String, ownerUserId: String, content: String) {
        val entity = NoteHistoryEntity(
            noteId = noteId,
            ownerUserId = ownerUserId,
            content = content,
            viewedAt = System.currentTimeMillis(),
        )
        noteHistoryDao.insert(entity)

        syncHistoryWithServer(noteId)
    }

    override fun getHistory(limit: Int): Flow<List<NoteHistory>> {
        return noteHistoryDao.getHistory(limit).map { entities ->
            entities.map { entity ->
                NoteHistory(
                    id = entity.id,
                    noteId = entity.noteId,
                    ownerUserId = entity.ownerUserId,
                    content = entity.content,
                    viewedAt = entity.viewedAt,
                )
            }
        }
    }

    override suspend fun clearHistory() {
        noteHistoryDao.clearHistory()
    }

    override suspend fun syncHistoryWithServer() {
        // This is called for full sync - individual record sync is handled in addToHistory
    }

    private suspend fun syncHistoryWithServer(noteId: String) {
        try {
            val session = sessionStore.currentSession()
            val token = session.accessToken ?: return

            noteHistoryApi.recordHistory(token, noteId)
        } catch (e: Exception) {
            // Silently fail - history recording is not critical
        }
    }
}
