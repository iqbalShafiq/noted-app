package id.usecase.noted.presentation.note.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

sealed interface NoteEditorBlock {
    val id: Long

    data class Text(
        override val id: Long,
        val value: TextFieldValue,
    ) : NoteEditorBlock

    data class Image(
        override val id: Long,
        val uri: String,
    ) : NoteEditorBlock

    data class Location(
        override val id: Long,
        val latitude: Double,
        val longitude: Double,
        val label: String,
    ) : NoteEditorBlock
}

fun createEmptyTextBlock(id: Long): NoteEditorBlock.Text {
    return NoteEditorBlock.Text(
        id = id,
        value = TextFieldValue(
            text = "",
            selection = TextRange(0),
        ),
    )
}
