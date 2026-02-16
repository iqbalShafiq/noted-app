package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteHistoryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.appendPathSegments
import io.ktor.http.parameters
import io.ktor.http.takeFrom

class KtorNoteHistoryApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : NoteHistoryApi {
    override suspend fun recordHistory(accessToken: String, noteId: String) {
        httpClient.post {
            url {
                takeFrom(baseUrl)
                appendPathSegments("api", "notes", noteId, "history")
            }
            bearerAuth(accessToken)
        }
    }

    override suspend fun getHistory(accessToken: String, limit: Int): List<NoteHistoryDto> {
        return httpClient.get {
            url {
                takeFrom(baseUrl)
                appendPathSegments("api", "notes", "history")
                parameters.append("limit", limit.toString())
            }
            bearerAuth(accessToken)
        }.body()
    }
}
