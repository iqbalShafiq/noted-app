package id.usecase.noted.feature.note.domain

data class Note(
    val id: Long,
    val content: String,
    val createdAt: Long,
)
