package nl.brul.api.common.error

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: List<FieldError> = emptyList()
)

@Serializable
data class FieldError(
    val field: String,
    val message: String
)