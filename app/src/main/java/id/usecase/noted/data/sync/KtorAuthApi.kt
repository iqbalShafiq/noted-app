package id.usecase.noted.data.sync

import id.usecase.noted.shared.auth.AuthForgotPasswordRequest
import id.usecase.noted.shared.auth.AuthForgotPasswordResponse
import id.usecase.noted.shared.auth.AuthLoginRequest
import id.usecase.noted.shared.auth.AuthRegisterRequest
import id.usecase.noted.shared.auth.AuthResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom

class KtorAuthApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : AuthApi {
    override suspend fun register(username: String, password: String): AuthResponse {
        return httpClient.post {
            url {
                takeFrom(baseUrl)
                appendPathSegments("api", "auth", "register")
            }
            setBody(AuthRegisterRequest(username = username, password = password))
        }.body()
    }

    override suspend fun login(username: String, password: String): AuthResponse {
        return httpClient.post {
            url {
                takeFrom(baseUrl)
                appendPathSegments("api", "auth", "login")
            }
            setBody(AuthLoginRequest(username = username, password = password))
        }.body()
    }

    override suspend fun forgotPassword(username: String, newPassword: String): String {
        val response: AuthForgotPasswordResponse = httpClient.post {
            url {
                takeFrom(baseUrl)
                appendPathSegments("api", "auth", "forgot-password")
            }
            setBody(
                AuthForgotPasswordRequest(
                    username = username,
                    newPassword = newPassword,
                ),
            )
        }.body()
        return response.message
    }
}
