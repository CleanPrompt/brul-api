package nl.brul.api.common.error

import io.ktor.http.HttpStatusCode

class AppException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val details: List<FieldError> = emptyList()
) : RuntimeException(message)