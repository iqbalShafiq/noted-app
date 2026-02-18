package id.usecase.backend

import id.usecase.noted.shared.auth.AuthForgotPasswordRequest
import id.usecase.noted.shared.auth.AuthForgotPasswordResponse
import id.usecase.noted.shared.auth.AuthLoginRequest
import id.usecase.noted.shared.auth.AuthRegisterRequest
import id.usecase.noted.shared.auth.AuthResponse
import id.usecase.noted.shared.note.AddHistoryRequest
import id.usecase.noted.shared.note.CreateNoteCommentRequest
import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.NoteCommentDto
import id.usecase.noted.shared.note.NoteCommentsPageDto
import id.usecase.noted.shared.note.NoteDto
import id.usecase.noted.shared.note.NoteEngagementDto
import id.usecase.noted.shared.note.NoteHistoryDto
import id.usecase.noted.shared.note.ShareNoteRequest
import id.usecase.noted.shared.note.ShareNoteResponse
import id.usecase.noted.shared.note.SyncMutationDto
import id.usecase.noted.shared.note.SyncMutationType
import id.usecase.noted.shared.note.SyncPullResponse
import id.usecase.noted.shared.note.SyncPushRequest
import id.usecase.noted.shared.note.SyncPushResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
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
            module(storageModeOverride = "memory")
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
            module(storageModeOverride = "memory")
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
            module(storageModeOverride = "memory")
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
            module(storageModeOverride = "memory")
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

    @Test
    fun testForgotPasswordEndpointResetsCredentials() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRegisterRequest("tester-forgot", "password123")))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val forgotResponse = client.post("/api/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    AuthForgotPasswordRequest(
                        username = "tester-forgot",
                        newPassword = "password456",
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, forgotResponse.status)
        val forgotBody = json.decodeFromString<AuthForgotPasswordResponse>(forgotResponse.bodyAsText())
        assertEquals("tester-forgot", forgotBody.username)

        val oldPasswordLogin = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthLoginRequest("tester-forgot", "password123")))
        }
        assertEquals(HttpStatusCode.BadRequest, oldPasswordLogin.status)

        val newPasswordLogin = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthLoginRequest("tester-forgot", "password456")))
        }
        assertEquals(HttpStatusCode.OK, newPasswordLogin.status)
    }

    @Test
    fun authenticatedUserCanLoveAndUnloveAccessibleExploreNote() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-engage-1", password = "password123")
        val reader = registerAndLogin(username = "reader-engage-1", password = "password123")

        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(
                json.encodeToString(
                    CreateNoteRequest(ownerUserId = "ignored", content = "Explorable note"),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())

        val shareResponse = client.post("/api/notes/${created.id}/share") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(ShareNoteRequest(recipientUserId = reader.userId)))
        }
        assertEquals(HttpStatusCode.Created, shareResponse.status)

        val engagementBeforeResponse = client.get("/api/notes/${created.id}/engagement") {
            bearerAuth(reader.accessToken)
        }
        assertEquals(HttpStatusCode.OK, engagementBeforeResponse.status)
        val engagementBefore = json.decodeFromString<NoteEngagementDto>(engagementBeforeResponse.bodyAsText())
        assertEquals(0, engagementBefore.loveCount)
        assertTrue(!engagementBefore.hasLovedByMe)

        val loveResponse = client.put("/api/notes/${created.id}/reactions/love") {
            bearerAuth(reader.accessToken)
        }
        assertEquals(HttpStatusCode.OK, loveResponse.status)
        val loved = json.decodeFromString<NoteEngagementDto>(loveResponse.bodyAsText())
        assertEquals(1, loved.loveCount)
        assertTrue(loved.hasLovedByMe)

        val unloveResponse = client.delete("/api/notes/${created.id}/reactions/love") {
            bearerAuth(reader.accessToken)
        }
        assertEquals(HttpStatusCode.OK, unloveResponse.status)
        val unloved = json.decodeFromString<NoteEngagementDto>(unloveResponse.bodyAsText())
        assertEquals(0, unloved.loveCount)
        assertTrue(!unloved.hasLovedByMe)
    }

    @Test
    fun authenticatedUserCanCreateAndFetchCommentsOnPublicOrLinkSharedNote() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-engage-2", password = "password123")
        val reader = registerAndLogin(username = "reader-engage-2", password = "password123")

        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(
                json.encodeToString(
                    CreateNoteRequest(ownerUserId = "ignored", content = "Comment target note"),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())

        val shareResponse = client.post("/api/notes/${created.id}/share") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(ShareNoteRequest(recipientUserId = reader.userId)))
        }
        assertEquals(HttpStatusCode.Created, shareResponse.status)

        val createCommentResponse = client.post("/api/notes/${created.id}/comments") {
            contentType(ContentType.Application.Json)
            bearerAuth(reader.accessToken)
            setBody(json.encodeToString(CreateNoteCommentRequest(content = "  Great note  ")))
        }
        assertEquals(HttpStatusCode.Created, createCommentResponse.status)
        val createdComment = json.decodeFromString<NoteCommentDto>(createCommentResponse.bodyAsText())
        assertEquals(created.id, createdComment.noteId)
        assertEquals(reader.userId, createdComment.authorUserId)
        assertEquals(reader.username, createdComment.authorUsername)
        assertEquals("Great note", createdComment.content)

        val listCommentsResponse = client.get("/api/notes/${created.id}/comments?limit=20") {
            bearerAuth(reader.accessToken)
        }
        assertEquals(HttpStatusCode.OK, listCommentsResponse.status)
        val commentsPage = json.decodeFromString<NoteCommentsPageDto>(listCommentsResponse.bodyAsText())
        assertEquals(1, commentsPage.items.size)
        assertEquals(createdComment.id, commentsPage.items.first().id)
        assertEquals(reader.username, commentsPage.items.first().authorUsername)

        val engagementResponse = client.get("/api/notes/${created.id}/engagement") {
            bearerAuth(reader.accessToken)
        }
        assertEquals(HttpStatusCode.OK, engagementResponse.status)
        val engagement = json.decodeFromString<NoteEngagementDto>(engagementResponse.bodyAsText())
        assertEquals(1, engagement.commentCount)
    }

    @Test
    fun privateNoteCommentRequestReturns404Or400ForUnauthorizedUser() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-engage-3", password = "password123")
        val outsider = registerAndLogin(username = "outsider-engage-3", password = "password123")

        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(
                json.encodeToString(
                    CreateNoteRequest(ownerUserId = "ignored", content = "Private note"),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())

        val createCommentResponse = client.post("/api/notes/${created.id}/comments") {
            contentType(ContentType.Application.Json)
            bearerAuth(outsider.accessToken)
            setBody(json.encodeToString(CreateNoteCommentRequest(content = "Should not pass")))
        }
        assertEquals(HttpStatusCode.BadRequest, createCommentResponse.status)
    }

    @Test
    fun noteHistoryRejectsRequestBodyNoteIdThatDiffersFromPath() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-history-1", password = "password123")

        val firstNoteCreateResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(
                json.encodeToString(
                    CreateNoteRequest(ownerUserId = "ignored", content = "History source note"),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, firstNoteCreateResponse.status)
        val firstNote = json.decodeFromString<NoteDto>(firstNoteCreateResponse.bodyAsText())

        val secondNoteCreateResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(
                json.encodeToString(
                    CreateNoteRequest(ownerUserId = "ignored", content = "Different note"),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, secondNoteCreateResponse.status)
        val secondNote = json.decodeFromString<NoteDto>(secondNoteCreateResponse.bodyAsText())

        val addHistoryResponse = client.post("/api/notes/${firstNote.id}/history") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(AddHistoryRequest(noteId = secondNote.id)))
        }
        assertEquals(HttpStatusCode.BadRequest, addHistoryResponse.status)

        val historyResponse = client.get("/api/notes/history") {
            bearerAuth(owner.accessToken)
        }
        assertEquals(HttpStatusCode.OK, historyResponse.status)
        val historyItems = json.decodeFromString<List<NoteHistoryDto>>(historyResponse.bodyAsText())
        assertTrue(historyItems.isEmpty())
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
