// nl/brul/api/common/error/ErrorHandling.kt
package nl.brul.api.common.error

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import nl.brul.api.features.auth.InvalidCredentialsException
import nl.brul.api.features.user.repository.DuplicateEmailException

fun Application.configureErrorHandling() {
    install(StatusPages) {

        exception<AppException> { call, cause ->
            call.respond(
                cause.status,
                ApiError(
                    code = cause.code,
                    message = cause.message,
                    details = cause.details
                )
            )
        }

        exception<DuplicateEmailException> { call, _ ->
            call.respond(
                HttpStatusCode.Conflict,
                ApiError(
                    code = "EMAIL_ALREADY_EXISTS",
                    message = "An account with this email address already exists."
                )
            )
        }

        exception<InvalidCredentialsException> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError(
                    code = "INVALID_CREDENTIALS",
                    message = "Invalid email or password."
                )
            )
        }

        exception<IllegalArgumentException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    code = "VALIDATION_ERROR",
                    message = "The request contains invalid fields."
                )
            )
        }

        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    code = "VALIDATION_ERROR",
                    message = "The request contains invalid fields."
                )
            )
        }

        exception<JsonConvertException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    code = "VALIDATION_ERROR",
                    message = "The request contains invalid fields."
                )
            )
        }

        exception<SerializationException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    code = "VALIDATION_ERROR",
                    message = "The request contains invalid fields."
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error(
                "Unexpected API error",
                cause
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(
                    code = "INTERNAL_ERROR",
                    message = "An unexpected error occurred."
                )
            )
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiError(
                    code = "NOT_FOUND",
                    message = "The requested endpoint was not found."
                )
            )
        }
    }
}