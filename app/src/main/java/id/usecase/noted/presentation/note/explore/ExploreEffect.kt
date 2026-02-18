package id.usecase.noted.presentation.note.explore

sealed interface ExploreEffect {
    data object NavigateBack : ExploreEffect
    data class NavigateToNoteDetail(val noteId: String) : ExploreEffect
    data class ShowMessage(val message: String) : ExploreEffect
}
