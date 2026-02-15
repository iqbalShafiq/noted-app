package id.usecase.backend.plugins

import id.usecase.backend.auth.presentation.registerAuthRoutes
import id.usecase.backend.auth.service.AuthService
import id.usecase.backend.note.presentation.registerNoteRoutes
import id.usecase.backend.note.service.NoteSharingService
import id.usecase.backend.sync.presentation.registerSyncRoutes
import id.usecase.backend.sync.service.NoteSyncService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

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

            registerAuthRoutes(authService = authService)
            registerNoteRoutes(noteSharingService = noteSharingService)
            registerSyncRoutes(noteSyncService = noteSyncService)
        }
    }
}
