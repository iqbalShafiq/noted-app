package id.usecase.noted.data.user

import id.usecase.noted.domain.user.UserRepository
import id.usecase.noted.shared.user.UpdateProfileRequest
import id.usecase.noted.shared.user.UserProfileDto
import id.usecase.noted.shared.user.UserStatisticsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserRepositoryImpl(
    private val userApi: UserApi,
) : UserRepository {
    private val _cachedProfile = MutableStateFlow<UserProfileDto?>(null)
    private val cachedProfile: StateFlow<UserProfileDto?> = _cachedProfile.asStateFlow()

    override suspend fun getProfile(): Result<Pair<UserProfileDto, UserStatisticsDto>> {
        return userApi.getProfile().map { response ->
            val pair = Pair(response.profile, response.statistics)
            _cachedProfile.value = response.profile
            pair
        }
    }

    override suspend fun updateProfile(
        displayName: String?,
        bio: String?,
        profilePictureUrl: String?,
        email: String?,
    ): Result<UserProfileDto> {
        val request = UpdateProfileRequest(
            displayName = displayName,
            bio = bio,
            profilePictureUrl = profilePictureUrl,
            email = email,
        )
        return userApi.updateProfile(request).map { response ->
            response.profile?.also { profile ->
                _cachedProfile.value = profile
            } ?: throw IllegalStateException("Profile not returned in response")
        }
    }

    override fun observeProfile(): Flow<UserProfileDto?> {
        return cachedProfile
    }
}
