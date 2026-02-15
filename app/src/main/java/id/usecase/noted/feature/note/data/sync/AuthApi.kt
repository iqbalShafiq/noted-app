package id.usecase.noted.feature.note.data.sync

import id.usecase.noted.shared.auth.AuthResponse

interface AuthApi {
    suspend fun register(username: String, password: String): AuthResponse

    suspend fun login(username: String, password: String): AuthResponse

    suspend fun forgotPassword(username: String, newPassword: String): String
}
