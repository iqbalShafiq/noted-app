package id.usecase.noted.shared.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthRegisterRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AuthLoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val userId: String,
    val username: String,
    val accessToken: String,
    val expiresInSeconds: Long,
)
