package id.usecase.noted.presentation.note.explore

sealed interface ExploreIntent {
    data object LoadExploreNotes : ExploreIntent
    data object RefreshExploreNotes : ExploreIntent
    data object NavigateBackClicked : ExploreIntent
    data class SearchQueryChanged(val query: String) : ExploreIntent
    data object SearchClicked : ExploreIntent
    data object DismissSearch : ExploreIntent
    data object ClearSearchHistory : ExploreIntent
}
