package id.usecase.backend.plugins

import id.usecase.backend.domain.auth.AuthRepository
import id.usecase.backend.presentation.auth.registerAuthRoutes
import id.usecase.backend.service.auth.AuthService
import id.usecase.backend.presentation.note.registerNoteRoutes
import id.usecase.backend.service.note.NoteHistoryService
import id.usecase.backend.service.note.NoteSharingService
import id.usecase.backend.presentation.sync.registerSyncRoutes
import id.usecase.backend.service.sync.NoteSyncService
import id.usecase.backend.presentation.user.userRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respond
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    noteSharingService: NoteSharingService,
    noteHistoryService: NoteHistoryService,
    noteSyncService: NoteSyncService,
    authService: AuthService,
    authRepository: AuthRepository,
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
        get("/docs") {
            call.respondRedirect("/docs/scalar.html")
        }
        staticResources("/docs", "static")

        staticResources("/openapi", "openapi")

        route("/api") {
            get("/health") {
                call.respond(mapOf("status" to "ok"))
            }

            registerAuthRoutes(authService = authService)
            registerNoteRoutes(
                noteSharingService = noteSharingService,
                noteHistoryService = noteHistoryService,
            )
            registerSyncRoutes(noteSyncService = noteSyncService)
            userRoutes(authRepository = authRepository)
        }
    }
}
