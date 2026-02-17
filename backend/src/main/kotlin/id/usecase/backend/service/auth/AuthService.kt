package id.usecase.backend.service.auth

import id.usecase.backend.domain.auth.AuthRepository
import id.usecase.backend.auth.security.JwtService
import id.usecase.noted.shared.auth.AuthForgotPasswordRequest
import id.usecase.noted.shared.auth.AuthForgotPasswordResponse
import id.usecase.noted.shared.auth.AuthLoginRequest
import id.usecase.noted.shared.auth.AuthRegisterRequest
import id.usecase.noted.shared.auth.AuthResponse
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory

class AuthService(
    private val authRepository: AuthRepository,
    private val jwtService: JwtService,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun register(request: AuthRegisterRequest): AuthResponse {
        val username = request.username.trim().lowercase()
        val password = request.password

        require(username.length >= 3) { "Username minimal 3 karakter" }
        require(password.length >= 8) { "Password minimal 8 karakter" }

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val created = authRepository.createUser(
            username = username,
            passwordHash = passwordHash,
            createdAtEpochMillis = clock(),
        )

        val token = jwtService.issueToken(
            userId = created.userId,
            username = created.username,
        )

        logger.info("auth.register success userId={} username={}", created.userId, created.username)

        return AuthResponse(
            userId = created.userId,
            username = created.username,
            accessToken = token,
            expiresInSeconds = jwtService.expiresInSeconds,
        )
    }

    suspend fun login(request: AuthLoginRequest): AuthResponse {
        val username = request.username.trim().lowercase()
        val password = request.password

        val user = authRepository.findByUsername(username)
            ?: throw IllegalArgumentException("Username atau password salah")

        val passwordMatched = BCrypt.checkpw(password, user.passwordHash)
        if (!passwordMatched) {
            throw IllegalArgumentException("Username atau password salah")
        }

        authRepository.updateLastLogin(user.userId)

        val token = jwtService.issueToken(
            userId = user.userId,
            username = user.username,
        )

        logger.info("auth.login success userId={} username={}", user.userId, user.username)

        return AuthResponse(
            userId = user.userId,
            username = user.username,
            accessToken = token,
            expiresInSeconds = jwtService.expiresInSeconds,
        )
    }

    suspend fun forgotPassword(request: AuthForgotPasswordRequest): AuthForgotPasswordResponse {
        val username = request.username.trim().lowercase()
        val newPassword = request.newPassword

        require(username.length >= 3) { "Username minimal 3 karakter" }
        require(newPassword.length >= 8) { "Password baru minimal 8 karakter" }

        val newPasswordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        val updatedUser = authRepository.updatePasswordHashByUsername(
            username = username,
            passwordHash = newPasswordHash,
        )

        logger.info("auth.forgot-password success userId={} username={}", updatedUser.userId, updatedUser.username)

        return AuthForgotPasswordResponse(
            username = updatedUser.username,
            message = "Password berhasil diperbarui. Silakan login ulang.",
        )
    }
}
