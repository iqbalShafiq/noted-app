package id.usecase.noted.data.user

import id.usecase.noted.shared.user.GetUserProfileResponse
import id.usecase.noted.shared.user.UpdateProfileRequest
import id.usecase.noted.shared.user.UpdateProfileResponse

interface UserApi {
    suspend fun getProfile(): Result<GetUserProfileResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<UpdateProfileResponse>
}
