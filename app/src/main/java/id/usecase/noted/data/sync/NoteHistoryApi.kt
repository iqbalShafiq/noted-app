package id.usecase.noted.data.sync

import id.usecase.noted.shared.note.NoteHistoryDto

interface NoteHistoryApi {
    suspend fun recordHistory(accessToken: String, noteId: String)

    suspend fun getHistory(accessToken: String, limit: Int): List<NoteHistoryDto>
}
