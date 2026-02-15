package id.usecase.noted.presentation.note.explore

sealed interface ExploreIntent {
    data object LoadExploreNotes : ExploreIntent
    data object RefreshExploreNotes : ExploreIntent
    data object NavigateBackClicked : ExploreIntent
}
