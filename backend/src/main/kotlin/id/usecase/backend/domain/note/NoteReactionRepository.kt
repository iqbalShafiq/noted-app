package id.usecase.backend.domain.note

data class NoteLoveSnapshot(
    val loveCount: Int,
    val hasLovedByMe: Boolean,
)

interface NoteReactionRepository {
    suspend fun addLove(noteId: String, userId: String): Boolean

    suspend fun removeLove(noteId: String, userId: String): Boolean

    suspend fun hasLoved(noteId: String, userId: String): Boolean

    suspend fun countLoves(noteId: String): Int

    suspend fun getLoveSnapshot(noteId: String, userId: String): NoteLoveSnapshot
}
