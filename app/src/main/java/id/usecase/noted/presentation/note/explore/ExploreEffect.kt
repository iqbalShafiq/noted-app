package id.usecase.noted.presentation.note.explore

sealed interface ExploreEffect {
    data object NavigateBack : ExploreEffect
    data class ShowMessage(val message: String) : ExploreEffect
}
