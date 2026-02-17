package id.usecase.backend.data.auth

import id.usecase.backend.domain.auth.AuthRepository
import id.usecase.backend.domain.auth.AuthUser
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryAuthRepository : AuthRepository {
    private val usersById = linkedMapOf<String, AuthUser>()
    private val userIdByUsername = linkedMapOf<String, String>()
    private val mutex = Mutex()

    override suspend fun createUser(username: String, passwordHash: String, createdAtEpochMillis: Long): AuthUser {
        return mutex.withLock {
            val normalizedUsername = username.lowercase()
            require(normalizedUsername.isNotBlank()) { "username must not be blank" }
            if (userIdByUsername.containsKey(normalizedUsername)) {
                throw IllegalArgumentException("Username already exists")
            }

            val user = AuthUser(
                userId = UUID.randomUUID().toString(),
                username = normalizedUsername,
                passwordHash = passwordHash,
                createdAtEpochMillis = createdAtEpochMillis,
            )
            usersById[user.userId] = user
            userIdByUsername[normalizedUsername] = user.userId
            user
        }
    }

    override suspend fun findByUsername(username: String): AuthUser? {
        return mutex.withLock {
            val normalizedUsername = username.lowercase()
            val userId = userIdByUsername[normalizedUsername] ?: return@withLock null
            usersById[userId]
        }
    }

    override suspend fun findById(userId: String): AuthUser? {
        return mutex.withLock {
            usersById[userId]
        }
    }

    override suspend fun updatePasswordHashByUsername(username: String, passwordHash: String): AuthUser {
        return mutex.withLock {
            val normalizedUsername = username.lowercase()
            val userId = userIdByUsername[normalizedUsername]
                ?: throw IllegalArgumentException("Username tidak ditemukan")
            val current = usersById[userId]
                ?: throw IllegalStateException("Auth user mapping is inconsistent")

            val updated = current.copy(
                passwordHash = passwordHash,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            usersById[userId] = updated
            updated
        }
    }

    override suspend fun updateProfile(
        userId: String,
        displayName: String?,
        bio: String?,
        profilePictureUrl: String?,
        email: String?,
    ): AuthUser {
        return mutex.withLock {
            val current = usersById[userId]
                ?: throw IllegalArgumentException("User tidak ditemukan")

            val updated = current.copy(
                displayName = displayName,
                bio = bio,
                profilePictureUrl = profilePictureUrl,
                email = email,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            usersById[userId] = updated
            updated
        }
    }

    override suspend fun updateLastLogin(userId: String): AuthUser {
        return mutex.withLock {
            val current = usersById[userId]
                ?: throw IllegalArgumentException("User tidak ditemukan")

            val updated = current.copy(
                lastLoginAtEpochMillis = System.currentTimeMillis(),
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            usersById[userId] = updated
            updated
        }
    }

    override suspend fun getUserStatistics(userId: String): id.usecase.backend.auth.domain.UserStatistics {
        return id.usecase.backend.auth.domain.UserStatistics(
            totalNotes = 0,
            notesShared = 0,
            notesReceived = 0,
            lastSyncAtEpochMillis = null
        )
    }
}
