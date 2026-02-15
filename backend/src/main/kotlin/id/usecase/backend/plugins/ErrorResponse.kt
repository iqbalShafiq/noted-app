package id.usecase.backend.plugins

import kotlinx.serialization.Serializable

@Serializable
internal data class ErrorResponse(val message: String)
