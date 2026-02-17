package id.usecase.backend.presentation.auth

import id.usecase.backend.service.auth.AuthService
import id.usecase.noted.shared.auth.AuthForgotPasswordRequest
import id.usecase.noted.shared.auth.AuthLoginRequest
import id.usecase.noted.shared.auth.AuthRegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.registerAuthRoutes(authService: AuthService) {
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

        post("/forgot-password") {
            val request = call.receive<AuthForgotPasswordRequest>()
            val response = authService.forgotPassword(request)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
