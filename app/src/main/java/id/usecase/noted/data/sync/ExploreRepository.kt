package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteDto
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

        runCatching {
            exploreApi.exploreNotes(accessToken = token, limit = limit)
        }.let { result ->
            emit(result)
        }
    }

    override fun searchExploreNotes(query: String, limit: Int): Flow<Result<List<NoteDto>>> = flow {
        val session = sessionStore.currentSession()
        val token = session.accessToken
            ?: throw IllegalStateException("Login diperlukan untuk pencarian")

        runCatching {
            exploreApi.searchExploreNotes(accessToken = token, query = query, limit = limit)
        }.let { result ->
            emit(result)
        }
    }

    override fun getNoteById(noteId: String): Flow<Result<NoteDto>> = flow {
        val session = sessionStore.currentSession()
        val token = session.accessToken
            ?: throw IllegalStateException("Login diperlukan untuk melihat note")

        runCatching {
            exploreApi.getNoteById(accessToken = token, noteId = noteId)
        }.let { result ->
            emit(result)
        }
    }
}
