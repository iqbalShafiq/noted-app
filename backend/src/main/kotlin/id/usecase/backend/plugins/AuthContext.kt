package id.usecase.backend.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

internal fun ApplicationCall.requireUserId(): String {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException("Unauthorized")
    val subject = principal.payload.subject
    if (subject.isNullOrBlank()) {
        throw UnauthorizedException("Unauthorized")
    }
    return subject
}

internal class UnauthorizedException(message: String) : RuntimeException(message)
