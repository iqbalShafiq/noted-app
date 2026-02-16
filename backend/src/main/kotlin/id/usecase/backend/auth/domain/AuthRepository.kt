package id.usecase.backend.auth.domain

data class AuthUser(
    val userId: String,
    val username: String,
    val passwordHash: String,
    val createdAtEpochMillis: Long,
    val displayName: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null,
    val email: String? = null,
    val lastLoginAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long = 0,
)

data class UserStatistics(
    val totalNotes: Int,
    val notesShared: Int,
    val notesReceived: Int,
    val lastSyncAtEpochMillis: Long?,
)

interface AuthRepository {
    suspend fun createUser(username: String, passwordHash: String, createdAtEpochMillis: Long): AuthUser

    suspend fun findByUsername(username: String): AuthUser?

    suspend fun findById(userId: String): AuthUser?

    suspend fun updatePasswordHashByUsername(username: String, passwordHash: String): AuthUser

    suspend fun updateProfile(
        userId: String,
        displayName: String?,
        bio: String?,
        profilePictureUrl: String?,
        email: String?,
    ): AuthUser

    suspend fun updateLastLogin(userId: String): AuthUser

    suspend fun getUserStatistics(userId: String): UserStatistics
}
