package id.usecase.backend.note.presentation

import id.usecase.backend.note.service.NoteSharingService
import id.usecase.backend.plugins.AUTH_JWT
import id.usecase.backend.plugins.ErrorResponse
import id.usecase.backend.plugins.requireUserId
import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.ShareNoteRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.registerNoteRoutes(noteSharingService: NoteSharingService) {
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
    }
}
