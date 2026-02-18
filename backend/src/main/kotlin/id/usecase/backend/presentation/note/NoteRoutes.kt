package id.usecase.backend.presentation.note

import id.usecase.backend.domain.auth.AuthRepository
import id.usecase.backend.service.note.NoteHistoryService
import id.usecase.backend.service.note.NoteEngagementService
import id.usecase.backend.service.note.NoteSharingService
import id.usecase.backend.domain.note.StoredNoteComment
import id.usecase.backend.domain.note.StoredNoteEngagement
import id.usecase.backend.service.note.StoredNoteCommentsPage
import id.usecase.backend.plugins.AUTH_JWT
import id.usecase.backend.plugins.ErrorResponse
import id.usecase.backend.plugins.requireUserId
import id.usecase.noted.shared.note.AddHistoryRequest
import id.usecase.noted.shared.note.CreateNoteCommentRequest
import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.NoteCommentDto
import id.usecase.noted.shared.note.NoteCommentsPageDto
import id.usecase.noted.shared.note.NoteEngagementDto
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
import io.ktor.server.routing.put

fun Route.registerNoteRoutes(
    noteSharingService: NoteSharingService,
    noteHistoryService: NoteHistoryService,
    noteEngagementService: NoteEngagementService,
    authRepository: AuthRepository,
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

        get("/notes/explore/search") {
            val userId = call.requireUserId()
            val query = call.request.queryParameters["query"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            if (query.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("query parameter is required"),
                )
                return@get
            }

            call.respond(noteSharingService.searchExploreNotes(query = query, excludeUserId = userId, limit = limit))
        }

        post("/notes/{noteId}/history") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val request = call.receive<AddHistoryRequest>()

            if (request.noteId != noteId) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Request noteId must match path noteId"),
                )
                return@post
            }

            try {
                noteHistoryService.recordHistory(userId = userId, noteId = noteId)
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

        get("/notes/{noteId}/engagement") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val engagement = noteEngagementService.getEngagement(userId = userId, noteId = noteId)
            call.respond(engagement.toDto())
        }

        put("/notes/{noteId}/reactions/love") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val engagement = noteEngagementService.addLove(userId = userId, noteId = noteId)
            call.respond(engagement.toDto())
        }

        delete("/notes/{noteId}/reactions/love") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val engagement = noteEngagementService.removeLove(userId = userId, noteId = noteId)
            call.respond(engagement.toDto())
        }

        get("/notes/{noteId}/comments") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val rawBefore = call.request.queryParameters["beforeEpochMillis"]
            val beforeEpochMillis = if (rawBefore == null) {
                null
            } else {
                rawBefore.toLongOrNull()
                    ?: throw IllegalArgumentException("beforeEpochMillis must be a valid long")
            }
            val page = noteEngagementService.listComments(
                userId = userId,
                noteId = noteId,
                limit = limit,
                beforeEpochMillis = beforeEpochMillis,
            )
            call.respond(page.toDto(authRepository = authRepository))
        }

        post("/notes/{noteId}/comments") {
            val userId = call.requireUserId()
            val noteId = call.parameters["noteId"] ?: throw IllegalArgumentException("noteId is required")
            val request = call.receive<CreateNoteCommentRequest>()
            val created = noteEngagementService.addComment(
                userId = userId,
                noteId = noteId,
                content = request.content,
            )
            call.respond(HttpStatusCode.Created, created.toDto(authRepository = authRepository))
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

private fun StoredNoteEngagement.toDto(): NoteEngagementDto {
    return NoteEngagementDto(
        noteId = noteId,
        loveCount = loveCount,
        hasLovedByMe = hasLovedByMe,
        commentCount = commentCount,
    )
}

private suspend fun StoredNoteComment.toDto(authRepository: AuthRepository): NoteCommentDto {
    val authorUsername = authRepository.findById(authorUserId)?.username ?: authorUserId
    return NoteCommentDto(
        id = id,
        noteId = noteId,
        authorUserId = authorUserId,
        authorUsername = authorUsername,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}

private suspend fun StoredNoteCommentsPage.toDto(authRepository: AuthRepository): NoteCommentsPageDto {
    return NoteCommentsPageDto(
        items = items.map { it.toDto(authRepository = authRepository) },
        nextBeforeEpochMillis = nextBeforeEpochMillis,
    )
}
