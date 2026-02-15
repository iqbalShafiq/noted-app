package id.usecase.noted.di

import id.usecase.noted.BuildConfig
import id.usecase.noted.feature.note.data.NoteRepository
import id.usecase.noted.feature.note.data.RoomNoteRepository
import id.usecase.noted.feature.note.data.local.NoteDatabase
import id.usecase.noted.feature.note.data.sync.AuthApi
import id.usecase.noted.feature.note.data.sync.KtorAuthApi
import id.usecase.noted.feature.note.data.sync.KtorNoteSyncApi
import id.usecase.noted.feature.note.data.sync.NetworkMonitor
import id.usecase.noted.feature.note.data.sync.NoteSyncApi
import id.usecase.noted.feature.note.data.sync.NoteSyncCoordinator
import id.usecase.noted.feature.note.data.sync.SessionStore
import id.usecase.noted.feature.auth.presentation.AuthViewModel
import id.usecase.noted.feature.note.presentation.editor.NoteEditorViewModel
import id.usecase.noted.feature.note.presentation.list.NoteListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal const val DEFAULT_BACKEND_BASE_URL = "http://10.0.2.2:8080"

internal fun resolveBackendBaseUrl(configuredUrl: String): String {
    val trimmedUrl = configuredUrl.trim()
    return if (trimmedUrl.isEmpty()) {
        DEFAULT_BACKEND_BASE_URL
    } else {
        trimmedUrl
    }
}

val appModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }

    single { NoteDatabase.getInstance(androidContext()) }
    single { get<NoteDatabase>().noteDao() }
    single { get<NoteDatabase>().syncCursorDao() }
    single { SessionStore(androidContext()) }
    single { NetworkMonitor(context = androidContext(), appScope = get()) }
    single<AuthApi> {
        KtorAuthApi(
            httpClient = get(),
            baseUrl = resolveBackendBaseUrl(BuildConfig.BACKEND_BASE_URL),
        )
    }
    single<NoteSyncApi> {
        KtorNoteSyncApi(
            httpClient = get(),
            baseUrl = resolveBackendBaseUrl(BuildConfig.BACKEND_BASE_URL),
        )
    }
    single {
        RoomNoteRepository(
            noteDao = get(),
            syncCursorDao = get(),
            syncApi = get(),
            authApi = get(),
            sessionStore = get(),
            networkMonitor = get(),
            appScope = get(),
        )
    }
    single<NoteRepository> { get<RoomNoteRepository>() }
    single<NoteSyncCoordinator> { get<RoomNoteRepository>() }

    viewModel {
        NoteListViewModel(
            noteRepository = get(),
            noteSyncCoordinator = get(),
        )
    }
    viewModel {
        NoteEditorViewModel(noteRepository = get())
    }
    viewModel {
        AuthViewModel(noteSyncCoordinator = get())
    }
}
