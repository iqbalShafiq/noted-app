package id.usecase.noted.presentation.note.list.preview

import id.usecase.noted.domain.NoteVisibility
import id.usecase.noted.presentation.note.list.NoteListItemUi
import id.usecase.noted.presentation.note.list.NoteListState

object NoteListPreviewData {
    val withItems = NoteListState(
        isLoading = false,
        myNotes = listOf(
            NoteListItemUi(
                id = 1,
                content = "Review requirement untuk sprint berikutnya.",
                createdAt = 1_738_700_000_000,
                visibility = NoteVisibility.PRIVATE,
            ),
            NoteListItemUi(
                id = 2,
                content = "Siapkan kebutuhan belanja mingguan.",
                createdAt = 1_738_650_000_000,
                visibility = NoteVisibility.LINK_SHARED,
            ),
        ),
        savedNotes = listOf(
            NoteListItemUi(
                id = 3,
                content = "Catatan penting dari meeting.",
                createdAt = 1_738_600_000_000,
                visibility = NoteVisibility.PUBLIC,
            ),
        ),
    )

    val empty = NoteListState(isLoading = false)
}
