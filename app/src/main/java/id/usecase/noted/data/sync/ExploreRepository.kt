package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ExploreRepository {
    fun exploreNotes(limit: Int = 50): Flow<Result<List<NoteDto>>>
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
}
