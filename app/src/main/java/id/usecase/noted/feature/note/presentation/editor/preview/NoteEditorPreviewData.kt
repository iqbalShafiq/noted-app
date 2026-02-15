package id.usecase.noted.feature.note.presentation.editor.preview

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import id.usecase.noted.feature.note.presentation.editor.NoteEditorBlock
import id.usecase.noted.feature.note.presentation.editor.NoteEditorState

object NoteEditorPreviewData {
    val idle = NoteEditorState(
        blocks = listOf(
            NoteEditorBlock.Text(
                id = 1,
                value = TextFieldValue(
                    text = "Checklist meeting produk hari Jumat.",
                    selection = TextRange(35),
                ),
            ),
            NoteEditorBlock.Image(
                id = 2,
                uri = "file:///android_asset/sample-note-image.jpg",
            ),
            NoteEditorBlock.Location(
                id = 3,
                latitude = -6.175392,
                longitude = 106.827153,
                label = "Monas",
            ),
            NoteEditorBlock.Text(
                id = 4,
                value = TextFieldValue(
                    text = "Tambahkan poin tindak lanjut setelah gambar.",
                    selection = TextRange(0),
                ),
            ),
        ),
        focusedTextBlockId = 4,
        isSaving = false,
    )

    val saving = NoteEditorState(
        blocks = listOf(
            NoteEditorBlock.Text(
                id = 1,
                value = TextFieldValue(""),
            ),
        ),
        focusedTextBlockId = 1,
        isSaving = true,
    )
}
