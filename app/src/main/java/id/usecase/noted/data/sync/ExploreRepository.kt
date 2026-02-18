package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ExploreRepository {
    fun exploreNotes(limit: Int = 50): Flow<Result<List<NoteDto>>>
    fun searchExploreNotes(query: String, limit: Int = 50): Flow<Result<List<NoteDto>>>
    fun getNoteById(noteId: String): Flow<Result<NoteDto>>
}

class SyncExploreRepository(
    private val exploreApi: ExploreApi,
    private val sessionStore: SessionStore,
) : ExploreRepository {
    override fun exploreNotes(limit: Int): Flow<Result<List<NoteDto>>> = flow {
        val session = sessionStore.currentSession()
        val token = session.accessToken
            ?: throw IllegalStateException("Login diperlukan untuk explore")

        try {
            emit(Result.success(exploreApi.exploreNotes(accessToken = token, limit = limit)))
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            emit(Result.failure(error))
        }
    }

    override fun searchExploreNotes(query: String, limit: Int): Flow<Result<List<NoteDto>>> = flow {
        val session = sessionStore.currentSession()
        val token = session.accessToken
            ?: throw IllegalStateException("Login diperlukan untuk pencarian")

        try {
            emit(Result.success(exploreApi.searchExploreNotes(accessToken = token, query = query, limit = limit)))
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            emit(Result.failure(error))
        }
    }

    override fun getNoteById(noteId: String): Flow<Result<NoteDto>> = flow {
        val session = sessionStore.currentSession()
        val token = session.accessToken
            ?: throw IllegalStateException("Login diperlukan untuk melihat note")

        try {
            emit(Result.success(exploreApi.getNoteById(accessToken = token, noteId = noteId)))
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            emit(Result.failure(error))
        }
    }
}
