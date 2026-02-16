package id.usecase.noted.navigation

import android.content.Intent
import android.net.Uri

sealed class DeepLink {
    data class NoteDetail(val noteId: String) : DeepLink()
    data object Unknown : DeepLink()
}

object DeepLinkHandler {
    private const val SCHEME_NOTED = "noted"
    private const val SCHEME_HTTPS = "https"
    private const val HOST_NOTED_APP = "noted.app"
    private const val PATH_NOTE = "/note/"

    fun parse(intent: Intent?): DeepLink {
        if (intent == null) return DeepLink.Unknown

        val data: Uri = intent.data ?: return DeepLink.Unknown
        val action = intent.action

        if (action != Intent.ACTION_VIEW) return DeepLink.Unknown

        return when {
            data.scheme == SCHEME_NOTED && data.host == "note" -> {
                val noteId = data.lastPathSegment
                if (noteId != null) {
                    DeepLink.NoteDetail(noteId)
                } else {
                    DeepLink.Unknown
                }
            }
            data.scheme == SCHEME_HTTPS && data.host == HOST_NOTED_APP -> {
                val path = data.path
                if (path != null && path.startsWith(PATH_NOTE)) {
                    val noteId = path.removePrefix(PATH_NOTE)
                    if (noteId.isNotEmpty()) {
                        DeepLink.NoteDetail(noteId)
                    } else {
                        DeepLink.Unknown
                    }
                } else {
                    DeepLink.Unknown
                }
            }
            else -> DeepLink.Unknown
        }
    }
}
