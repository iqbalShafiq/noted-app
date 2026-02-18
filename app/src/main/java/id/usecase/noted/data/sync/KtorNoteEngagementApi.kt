package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.CreateNoteCommentRequest
import id.usecase.noted.shared.note.NoteCommentDto
import id.usecase.noted.shared.note.NoteCommentsPageDto
import id.usecase.noted.shared.note.NoteEngagementDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.parameters
import io.ktor.http.takeFrom

class KtorNoteEngagementApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : NoteEngagementApi {
    override suspend fun getEngagement(accessToken: String, noteId: String): NoteEngagementDto {
        return safeApiCall {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("api", "notes", noteId, "engagement")
                }
                bearerAuth(accessToken)
            }.body()
        }
    }

    override suspend fun addLove(accessToken: String, noteId: String): NoteEngagementDto {
        return safeApiCall {
            httpClient.put {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("api", "notes", noteId, "reactions", "love")
                }
                bearerAuth(accessToken)
            }.body()
        }
    }

    override suspend fun removeLove(accessToken: String, noteId: String): NoteEngagementDto {
        return safeApiCall {
            httpClient.delete {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("api", "notes", noteId, "reactions", "love")
                }
                bearerAuth(accessToken)
            }.body()
        }
    }

    override suspend fun getComments(
        accessToken: String,
        noteId: String,
        limit: Int,
        beforeEpochMillis: Long?,
    ): NoteCommentsPageDto {
        return safeApiCall {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("api", "notes", noteId, "comments")
                    parameters.append("limit", limit.toString())
                    if (beforeEpochMillis != null) {
                        parameters.append("beforeEpochMillis", beforeEpochMillis.toString())
                    }
                }
                bearerAuth(accessToken)
            }.body()
        }
    }

    override suspend fun addComment(accessToken: String, noteId: String, content: String): NoteCommentDto {
        return safeApiCall {
            httpClient.post {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("api", "notes", noteId, "comments")
                }
                bearerAuth(accessToken)
                setBody(CreateNoteCommentRequest(content = content))
            }.body()
        }
    }
}

private suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): T {
    return try {
        block()
    } catch (error: ClientRequestException) {
        throw IllegalStateException(error.response.status.toReadableMessage(), error)
    } catch (error: ServerResponseException) {
        throw IllegalStateException("Terjadi gangguan server", error)
    }
}

private fun HttpStatusCode.toReadableMessage(): String {
    return when (this) {
        HttpStatusCode.NotFound -> "Not found"
        HttpStatusCode.Unauthorized -> "Unauthorized"
        else -> "Permintaan gagal"
    }
}
