package id.usecase.noted.domain

enum class NoteVisibility {
    PRIVATE,
    LINK_SHARED,
    PUBLIC;

    companion object {
        fun fromString(value: String): NoteVisibility =
            entries.find { it.name == value } ?: PRIVATE
    }
}
