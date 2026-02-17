package id.usecase.backend.sync.presentation

import id.usecase.backend.plugins.AUTH_JWT
import id.usecase.backend.plugins.requireUserId
import id.usecase.backend.sync.service.NoteSyncService
import id.usecase.noted.shared.note.SyncPushRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.registerSyncRoutes(noteSyncService: NoteSyncService) {
    authenticate(AUTH_JWT) {
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
