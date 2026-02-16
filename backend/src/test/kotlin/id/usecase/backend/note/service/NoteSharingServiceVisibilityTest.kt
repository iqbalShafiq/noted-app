package id.usecase.backend.note.service

import id.usecase.backend.module
import id.usecase.noted.shared.auth.AuthRegisterRequest
import id.usecase.noted.shared.auth.AuthResponse
import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.NoteDto
import id.usecase.noted.shared.note.ShareNoteRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NoteSharingServiceVisibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `exploreNotes only returns PUBLIC notes`() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-1", password = "password123")
        val explorer = registerAndLogin(username = "explorer", password = "password123")

        // Create a note (defaults to PRIVATE)
        client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(CreateNoteRequest(ownerUserId = "ignored", content = "Private note")))
        }

        // Explorer should not see the private note
        val exploreResponse = client.get("/api/notes/explore") {
            bearerAuth(explorer.accessToken)
        }
        assertEquals(HttpStatusCode.OK, exploreResponse.status)
        val notes = json.decodeFromString<List<NoteDto>>(exploreResponse.bodyAsText())
        assertEquals(0, notes.size)
    }

    @Test
    fun `exploreNotes excludes notes owned by requesting user`() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-2", password = "password123")

        // Create a note
        client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(CreateNoteRequest(ownerUserId = "ignored", content = "My note")))
        }

        // Owner should not see their own note in explore
        val exploreResponse = client.get("/api/notes/explore") {
            bearerAuth(owner.accessToken)
        }
        val notes = json.decodeFromString<List<NoteDto>>(exploreResponse.bodyAsText())
        assertEquals(0, notes.size)
    }

    @Test
    fun `getNoteByLink returns PUBLIC note to any user`() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-3", password = "password123")
        val other = registerAndLogin(username = "other-1", password = "password123")

        // Create and share a note to make it accessible
        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(CreateNoteRequest(ownerUserId = "ignored", content = "Test note")))
        }
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())

        // Share with other user first so they can access it
        client.post("/api/notes/${created.id}/share") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(ShareNoteRequest(recipientUserId = other.userId)))
        }

        // Other user can access the shared note
        val detailResponse = client.get("/api/notes/${created.id}") {
            bearerAuth(other.accessToken)
        }
        assertEquals(HttpStatusCode.OK, detailResponse.status)
        val note = json.decodeFromString<NoteDto>(detailResponse.bodyAsText())
        assertEquals(created.id, note.id)
    }

    @Test
    fun `getNoteByLink returns LINK_SHARED note to any user`() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-4", password = "password123")
        val other = registerAndLogin(username = "other-2", password = "password123")

        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(CreateNoteRequest(ownerUserId = "ignored", content = "Link shared note")))
        }
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())

        // Share with other user
        client.post("/api/notes/${created.id}/share") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(ShareNoteRequest(recipientUserId = other.userId)))
        }

        // Other user can access
        val detailResponse = client.get("/api/notes/${created.id}") {
            bearerAuth(other.accessToken)
        }
        assertEquals(HttpStatusCode.OK, detailResponse.status)
    }

    @Test
    fun `getNoteByLink returns null for PRIVATE note to non-owner`() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-5", password = "password123")
        val other = registerAndLogin(username = "other-3", password = "password123")

        // Create note (PRIVATE by default)
        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(CreateNoteRequest(ownerUserId = "ignored", content = "Private note")))
        }
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())

        // Other user cannot access unshared private note
        val detailResponse = client.get("/api/notes/${created.id}") {
            bearerAuth(other.accessToken)
        }
        // Non-shared private notes return 404
        assertEquals(HttpStatusCode.NotFound, detailResponse.status)
    }

    @Test
    fun `getNoteByLink returns PRIVATE note to owner`() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val owner = registerAndLogin(username = "owner-6", password = "password123")

        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            bearerAuth(owner.accessToken)
            setBody(json.encodeToString(CreateNoteRequest(ownerUserId = "ignored", content = "My private note")))
        }
        val created = json.decodeFromString<NoteDto>(createResponse.bodyAsText())

        // Owner can access their own private note
        val detailResponse = client.get("/api/notes/${created.id}") {
            bearerAuth(owner.accessToken)
        }
        assertEquals(HttpStatusCode.OK, detailResponse.status)
        val note = json.decodeFromString<NoteDto>(detailResponse.bodyAsText())
        assertEquals(created.id, note.id)
        assertEquals(owner.userId, note.ownerUserId)
    }

    @Test
    fun `getNoteByLink returns null for non-existent note`() = testApplication {
        application {
            module(storageModeOverride = "memory")
        }

        val user = registerAndLogin(username = "user-7", password = "password123")

        // Try to access non-existent note via public endpoint
        val detailResponse = client.get("/api/notes/public/non-existent-id") {
            bearerAuth(user.accessToken)
        }
        assertEquals(HttpStatusCode.NotFound, detailResponse.status)
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.registerAndLogin(
        username: String,
        password: String
    ): AuthResponse {
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRegisterRequest(username = username, password = password)))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        return json.decodeFromString(registerResponse.bodyAsText())
    }
}
