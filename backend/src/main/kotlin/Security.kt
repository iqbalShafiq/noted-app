package id.usecase

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.event.Level

const val AUTH_JWT = "auth-jwt"

fun Application.configureSecurity(jwtService: JwtService) {
    install(CallLogging) {
        level = Level.INFO
    }

    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = jwtService.realm()
            verifier(jwtService.verifier())
            validate { credential ->
                val subject = credential.payload.subject
                if (subject.isNullOrBlank()) {
                    null
                } else {
                    io.ktor.server.auth.jwt.JWTPrincipal(credential.payload)
                }
            }
        }
    }
}
