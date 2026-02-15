package id.usecase

import id.usecase.noted.shared.auth.AuthLoginRequest
import id.usecase.noted.shared.auth.AuthRegisterRequest
import id.usecase.noted.shared.auth.AuthResponse
import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.NoteDto
import id.usecase.noted.shared.note.ShareNoteRequest
import id.usecase.noted.shared.note.ShareNoteResponse
import id.usecase.noted.shared.note.SyncMutationDto
import id.usecase.noted.shared.note.SyncMutationType
import id.usecase.noted.shared.note.SyncPullResponse
import id.usecase.noted.shared.note.SyncPushRequest
import id.usecase.noted.shared.note.SyncPushResponse
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class ApplicationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testCreateAndFetchNote() = testApplication {
        application {
            module(appServices = inMemoryServices())
        }

        val auth = registerAndLogin(username = "owner-1", password = "password123")

        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(auth.accessToken)
            setBody(
                json.encodeToString(
                    CreateNoteRequest(ownerUserId = "ignored", content = "Belajar repository pattern"),
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())
        assertEquals(auth.userId, created.ownerUserId)
        assertEquals("Belajar repository pattern", created.content)

        val detailResponse = client.get("/api/notes/${created.id}") {
            bearerAuth(auth.accessToken)
        }
        assertEquals(HttpStatusCode.OK, detailResponse.status)
        val fetched = json.decodeFromString<NoteDto>(detailResponse.bodyAsText())
        assertEquals(created.id, fetched.id)
        assertTrue(fetched.sharedWithUserIds.isEmpty())
    }

    @Test
    fun testShareAndQuerySharedNotes() = testApplication {
        application {
            module(appServices = inMemoryServices())
        }

        val owner = registerAndLogin(username = "owner-2", password = "password123")
        val teammate = registerAndLogin(username = "teammate-1", password = "password123")

        val createdNote = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(
                json.encodeToString(
                    CreateNoteRequest(ownerUserId = "ignored", content = "Rencana sprint"),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, createdNote.status)

        val created = json.decodeFromString<NoteDto>(createdNote.bodyAsText())
        val shareResponse = client.post("/api/notes/${created.id}/share") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(ShareNoteRequest(recipientUserId = teammate.userId)))
        }

        assertEquals(HttpStatusCode.Created, shareResponse.status)
        val shared = json.decodeFromString<ShareNoteResponse>(shareResponse.bodyAsText())
        assertEquals(created.id, shared.noteId)
        assertEquals(teammate.userId, shared.recipientUserId)

        val sharedWithResponse = client.get("/api/notes/shared-with-me") {
            bearerAuth(teammate.accessToken)
        }
        assertEquals(HttpStatusCode.OK, sharedWithResponse.status)
        val sharedNotes = json.decodeFromString<List<NoteDto>>(sharedWithResponse.bodyAsText())
        assertEquals(1, sharedNotes.size)
        assertEquals(created.id, sharedNotes.first().id)

        val ownerNotesResponse = client.get("/api/notes") {
            bearerAuth(owner.accessToken)
        }
        assertEquals(HttpStatusCode.OK, ownerNotesResponse.status)
        val ownerNotes = json.decodeFromString<List<NoteDto>>(ownerNotesResponse.bodyAsText())
        assertEquals(1, ownerNotes.size)
        assertEquals(listOf(teammate.userId), ownerNotes.first().sharedWithUserIds)
    }

    @Test
    fun testSyncPushAndPull() = testApplication {
        application {
            module(appServices = inMemoryServices())
        }

        val auth = registerAndLogin(username = "owner-sync", password = "password123")

        val pushResponse = client.post("/api/sync/push") {
            contentType(ContentType.Application.Json)
            bearerAuth(auth.accessToken)
            setBody(
                json.encodeToString(
                    SyncPushRequest(
                        deviceId = "device-1",
                        mutations = listOf(
                            SyncMutationDto(
                                operationId = "op-1",
                                noteId = "note-sync-1",
                                type = SyncMutationType.UPSERT,
                                content = "Catatan sinkron",
                                clientUpdatedAtEpochMillis = 1_000L,
                                baseVersion = null,
                            ),
                        ),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, pushResponse.status)

        val pushed = json.decodeFromString<SyncPushResponse>(pushResponse.bodyAsText())
        assertEquals(listOf("op-1"), pushed.acceptedOperationIds)
        assertTrue(pushed.conflicts.isEmpty())
        assertEquals(1, pushed.appliedNotes.size)

        val pullResponse = client.get("/api/sync/pull?cursor=0") {
            bearerAuth(auth.accessToken)
        }
        assertEquals(HttpStatusCode.OK, pullResponse.status)

        val pulled = json.decodeFromString<SyncPullResponse>(pullResponse.bodyAsText())
        assertEquals(auth.userId, pulled.userId)
        assertEquals(1, pulled.notes.size)
        assertEquals("note-sync-1", pulled.notes.first().noteId)
        assertEquals("Catatan sinkron", pulled.notes.first().content)
    }

    @Test
    fun testLoginEndpoint() = testApplication {
        application {
            module(appServices = inMemoryServices())
        }

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRegisterRequest("tester-login", "password123")))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthLoginRequest("tester-login", "password123")))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val auth = json.decodeFromString<AuthResponse>(loginResponse.bodyAsText())
        assertTrue(auth.accessToken.isNotBlank())
        assertEquals("tester-login", auth.username)
    }

    private fun inMemoryServices(): AppServices {
        val noteRepository = InMemoryNoteRepository()
        val jwtService = JwtService(
            config = JwtConfig(
                issuer = "test-issuer",
                audience = "test-audience",
                realm = "test",
                secret = "test-secret-key-for-jwt",
                expiresInSeconds = 3600,
            ),
        )
        return NoteSharingService(
            noteRepository = noteRepository,
            noteShareRepository = InMemoryNoteShareRepository(),
        ).let { sharingService ->
            AppServices(
                noteSharingService = sharingService,
                noteSyncService = NoteSyncService(noteSyncRepository = noteRepository),
                authService = AuthService(
                    authRepository = InMemoryAuthRepository(),
                    jwtService = jwtService,
                ),
                jwtService = jwtService,
            )
        }
    }

    private suspend fun ApplicationTestBuilder.registerAndLogin(
        username: String,
        password: String,
    ): AuthResponse {
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRegisterRequest(username = username, password = password)))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        return json.decodeFromString(registerResponse.bodyAsText())
    }
}
