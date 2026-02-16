package id.usecase.noted.data.user

import id.usecase.noted.shared.user.GetUserProfileResponse
import id.usecase.noted.shared.user.UpdateProfileRequest
import id.usecase.noted.shared.user.UpdateProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom

class KtorUserApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : UserApi {
    override suspend fun getProfile(): Result<GetUserProfileResponse> {
        return safeApiCall {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("api", "user", "profile")
                }
            }.body()
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<UpdateProfileResponse> {
        return safeApiCall {
            httpClient.put {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("api", "user", "profile")
                }
                setBody(request)
            }.body()
        }
    }
}

private inline fun <T> safeApiCall(
    crossinline block: suspend () -> T,
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
