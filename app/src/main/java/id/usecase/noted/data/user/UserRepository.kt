package id.usecase.noted.data.user

import id.usecase.noted.shared.user.GetUserProfileResponse
import id.usecase.noted.shared.user.UpdateProfileRequest
import id.usecase.noted.shared.user.UpdateProfileResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface UserRepository {
    fun getProfile(): Flow<Result<GetUserProfileResponse>>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<UpdateProfileResponse>
}

class SyncUserRepository(
    private val userApi: UserApi,
) : UserRepository {
    override fun getProfile(): Flow<Result<GetUserProfileResponse>> = flow {
        emit(userApi.getProfile())
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<UpdateProfileResponse> {
        return userApi.updateProfile(request)
    }
}
