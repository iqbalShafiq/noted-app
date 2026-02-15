package id.usecase.noted.feature.note.presentation.list.preview

import id.usecase.noted.feature.note.presentation.list.NoteListItemUi
import id.usecase.noted.feature.note.presentation.list.NoteListState

object NoteListPreviewData {
    val withItems = NoteListState(
        isLoading = false,
        notes = listOf(
            NoteListItemUi(
                id = 1,
                content = "Review requirement untuk sprint berikutnya.",
                createdAt = 1_738_700_000_000,
            ),
            NoteListItemUi(
                id = 2,
                content = "Siapkan kebutuhan belanja mingguan.",
                createdAt = 1_738_650_000_000,
            ),
        ),
    )

    val empty = NoteListState(isLoading = false)
}
