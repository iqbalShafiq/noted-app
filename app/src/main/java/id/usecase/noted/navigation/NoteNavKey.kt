package id.usecase.noted.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NoteListNavKey : NavKey

@Serializable
data object AuthLoginNavKey : NavKey

@Serializable
data object AuthRegisterNavKey : NavKey

@Serializable
data object AuthForgotPasswordNavKey : NavKey

@Serializable
data class NoteEditorNavKey(
    val noteId: Long? = null,
) : NavKey

@Serializable
data object NoteCameraNavKey : NavKey

@Serializable
data class NoteLocationPickerNavKey(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val label: String? = null,
) : NavKey
