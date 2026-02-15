package id.usecase.backend.auth.domain

data class AuthUser(
    val userId: String,
    val username: String,
    val passwordHash: String,
    val createdAtEpochMillis: Long,
)

interface AuthRepository {
    suspend fun createUser(username: String, passwordHash: String, createdAtEpochMillis: Long): AuthUser

    suspend fun findByUsername(username: String): AuthUser?

    suspend fun findById(userId: String): AuthUser?
}
