package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header

interface ExploreApi {
    suspend fun exploreNotes(accessToken: String, limit: Int = 50): List<NoteDto>
    suspend fun getNoteById(accessToken: String, noteId: String): NoteDto
}

class KtorExploreApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : ExploreApi {
    override suspend fun exploreNotes(accessToken: String, limit: Int): List<NoteDto> {
        return httpClient.get("$baseUrl/notes/explore") {
            header("Authorization", "Bearer $accessToken")
            url {
                parameters.append("limit", limit.toString())
            }
        }.body()
    }

    override suspend fun getNoteById(accessToken: String, noteId: String): NoteDto {
        return httpClient.get("$baseUrl/notes/$noteId") {
            header("Authorization", "Bearer $accessToken")
        }.body()
    }
}
