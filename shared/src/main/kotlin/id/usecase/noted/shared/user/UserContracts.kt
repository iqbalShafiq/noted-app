package id.usecase.noted.shared.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val userId: String,
    val username: String,
    val displayName: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null,
    val email: String? = null,
    val createdAtEpochMillis: Long,
    val lastLoginAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null,
    val email: String? = null,
)

@Serializable
data class UpdateProfileResponse(
    val success: Boolean,
    val message: String,
    val profile: UserProfileDto? = null,
)

@Serializable
data class UserStatisticsDto(
    val totalNotes: Int,
    val notesShared: Int,
    val notesReceived: Int,
    val lastSyncAtEpochMillis: Long? = null,
)

@Serializable
data class GetUserProfileResponse(
    val profile: UserProfileDto,
    val statistics: UserStatisticsDto,
)
