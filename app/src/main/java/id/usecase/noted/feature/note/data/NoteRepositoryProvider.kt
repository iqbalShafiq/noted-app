package id.usecase.noted.feature.note.data

import android.content.Context
import id.usecase.noted.feature.note.data.local.NoteDatabase
import id.usecase.noted.feature.note.data.sync.KtorAuthApi
import id.usecase.noted.feature.note.data.sync.KtorNoteSyncApi
import id.usecase.noted.feature.note.data.sync.NetworkMonitor
import id.usecase.noted.feature.note.data.sync.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

object NoteRepositoryProvider {
    private const val DEFAULT_BACKEND_BASE_URL = "http://10.0.2.2:8080"

    @Volatile
    private var repository: RoomNoteRepository? = null

    fun provide(context: Context): RoomNoteRepository {
        return repository ?: synchronized(this) {
            repository ?: createRepository(context.applicationContext).also { created ->
                repository = created
            }
        }
    }

    private fun createRepository(context: Context): RoomNoteRepository {
        val database = NoteDatabase.getInstance(context)
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }
        }

        return RoomNoteRepository(
            noteDao = database.noteDao(),
            syncCursorDao = database.syncCursorDao(),
            syncApi = KtorNoteSyncApi(
                httpClient = httpClient,
                baseUrl = DEFAULT_BACKEND_BASE_URL,
            ),
            authApi = KtorAuthApi(
                httpClient = httpClient,
                baseUrl = DEFAULT_BACKEND_BASE_URL,
            ),
            sessionStore = SessionStore(context),
            networkMonitor = NetworkMonitor(
                context = context,
                appScope = appScope,
            ),
            appScope = appScope,
        )
    }
}
