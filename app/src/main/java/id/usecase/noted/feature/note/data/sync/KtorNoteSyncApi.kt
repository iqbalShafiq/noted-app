package id.usecase.noted.feature.note.data.sync

import id.usecase.noted.shared.note.SyncPullResponse
import id.usecase.noted.shared.note.SyncPushRequest
import id.usecase.noted.shared.note.SyncPushResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import io.ktor.http.parameters
import io.ktor.http.takeFrom

class KtorNoteSyncApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : NoteSyncApi {
    override suspend fun push(accessToken: String, request: SyncPushRequest): SyncPushResponse {
        return httpClient.post {
            url {
                takeFrom(baseUrl)
                appendPathSegments("api", "sync", "push")
            }
            bearerAuth(accessToken)
            setBody(request)
        }.body()
    }

    override suspend fun pull(accessToken: String, cursor: Long): SyncPullResponse {
        return httpClient.get {
            url {
                takeFrom(baseUrl)
                appendPathSegments("api", "sync", "pull")
                parameters.append("cursor", cursor.toString())
            }
            bearerAuth(accessToken)
        }.body()
    }
}
