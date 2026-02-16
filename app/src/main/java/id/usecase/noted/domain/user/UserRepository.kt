package id.usecase.noted.domain.user

import id.usecase.noted.shared.user.UserProfileDto
import id.usecase.noted.shared.user.UserStatisticsDto
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getProfile(): Result<Pair<UserProfileDto, UserStatisticsDto>>
    suspend fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        profilePictureUrl: String? = null,
        email: String? = null,
    ): Result<UserProfileDto>
    fun observeProfile(): Flow<UserProfileDto?>
}
