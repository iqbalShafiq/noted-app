package id.usecase

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import id.usecase.noted.shared.auth.AuthLoginRequest
import id.usecase.noted.shared.auth.AuthRegisterRequest
import id.usecase.noted.shared.note.CreateNoteRequest
import id.usecase.noted.shared.note.ShareNoteRequest
import id.usecase.noted.shared.note.SyncPushRequest
import kotlinx.serialization.Serializable

fun Application.configureRouting(
    noteSharingService: NoteSharingService,
    noteSyncService: NoteSyncService,
    authService: AuthService,
) {
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = ErrorResponse(cause.message ?: "Unauthorized"),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorResponse(cause.message ?: "Invalid request"),
            )
        }
        exception<Throwable> { call, _ ->
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse("Internal server error"),
            )
        }
    }

    routing {
        route("/api") {
            get("/health") {
                call.respond(mapOf("status" to "ok"))
            }

            route("/auth") {
                post("/register") {
                    val request = call.receive<AuthRegisterRequest>()
                    val response = authService.register(request)
                    call.respond(HttpStatusCode.Created, response)
                }

                post("/login") {
                    val request = call.receive<AuthLoginRequest>()
                    val response = authService.login(request)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            authenticate(AUTH_JWT) {
                post("/notes") {
                    val userId = call.requireUserId()
                    val request = call.receive<CreateNoteRequest>()
                    val created = noteSharingService.createNote(
                        request.copy(ownerUserId = userId),
                    )
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

                post("/sync/push") {
                    val userId = call.requireUserId()
                    val request = call.receive<SyncPushRequest>()
                    val response = noteSyncService.push(
                        request = request,
                        userId = userId,
                    )
                    call.respond(HttpStatusCode.OK, response)
                }

                get("/sync/pull") {
                    val userId = call.requireUserId()
                    val rawCursor = call.request.queryParameters["cursor"]
                    val cursor = when {
                        rawCursor == null -> 0L
                        else -> rawCursor.toLongOrNull()
                            ?: throw IllegalArgumentException("cursor query parameter must be a valid number")
                    }
                    val response = noteSyncService.pull(userId = userId, cursor = cursor)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}

private fun ApplicationCall.requireUserId(): String {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException("Unauthorized")
    val subject = principal.payload.subject
    if (subject.isNullOrBlank()) {
        throw UnauthorizedException("Unauthorized")
    }
    return subject
}

private class UnauthorizedException(message: String) : RuntimeException(message)

@Serializable
private data class ErrorResponse(val message: String)
