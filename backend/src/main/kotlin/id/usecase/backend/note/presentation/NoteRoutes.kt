package id.usecase.backend.note.presentation

import id.usecase.backend.note.service.NoteHistoryService
import id.usecase.backend.note.service.NoteSharingService
import id.usecase.backend.plugins.AUTH_JWT
import id.usecase.backend.plugins.ErrorResponse
import id.usecase.backend.plugins.requireUserId
import id.usecase.noted.shared.note.AddHistoryRequest
import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.NoteHistoryDto
import id.usecase.noted.shared.note.ShareNoteRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.registerNoteRoutes(
    noteSharingService: NoteSharingService,
    noteHistoryService: NoteHistoryService,
) {
    authenticate(AUTH_JWT) {
        post("/notes") {
            val userId = call.requireUserId()
            val request = call.receive<CreateNoteRequest>()
            val created = noteSharingService.createNote(request.copy(ownerUserId = userId))
            call.respond(HttpStatusCode.Created, created)
        }

        get("/notes/{noteId}") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val note = noteSharingService.getAccessibleNote(
                userId = userId,
                noteId = noteId,
            )

            if (note == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Note with id '$noteId' was not found"),
                )
                return@get
            }

            call.respond(note)
        }

        get("/notes") {
            val userId = call.requireUserId()
            call.respond(noteSharingService.getOwnedNotes(userId))
        }

        post("/notes/{noteId}/share") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val request = call.receive<ShareNoteRequest>()
            val shareResult = noteSharingService.shareNoteAsOwner(
                ownerUserId = userId,
                noteId = noteId,
                request = request,
            )

            if (shareResult == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Cannot share missing note with id '$noteId'"),
                )
                return@post
            }

            call.respond(HttpStatusCode.Created, shareResult)
        }

        get("/notes/shared-with-me") {
            val userId = call.requireUserId()
            call.respond(noteSharingService.getSharedWith(userId))
        }

        get("/notes/explore") {
            val userId = call.requireUserId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            call.respond(noteSharingService.exploreNotes(excludeUserId = userId, limit = limit))
        }

        post("/notes/{noteId}/history") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val request = call.receive<AddHistoryRequest>()

            try {
                noteHistoryService.recordHistory(userId = userId, noteId = request.noteId)
                call.respond(HttpStatusCode.Created, mapOf("success" to true))
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(e.message ?: "Note not found"),
                )
            }
        }

        get("/notes/history") {
            val userId = call.requireUserId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val history = noteHistoryService.getUserHistory(userId = userId, limit = limit)
            call.respond(
                history.map {
                    NoteHistoryDto(
                        noteId = it.noteId,
                        ownerUserId = it.noteOwnerId,
                        content = it.content,
                        viewedAtEpochMillis = it.viewedAtEpochMillis,
                    )
                }
            )
        }

        delete("/notes/history") {
            val userId = call.requireUserId()
            noteHistoryService.clearUserHistory(userId = userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    get("/notes/public/{noteId}") {
        val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
        val requestingUserId = runCatching { call.requireUserId() }.getOrNull()

        val note = noteSharingService.getNoteByLink(
            noteId = noteId,
            requestingUserId = requestingUserId,
        )

        if (note == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Note with id '$noteId' was not found or is not accessible"),
            )
            return@get
        }

        call.respond(note)
    }
}
