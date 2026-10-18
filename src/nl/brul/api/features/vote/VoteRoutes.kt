// src/nl/brul/api/features/vote/VoteRoutes.kt

package nl.brul.api.features.vote

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import nl.brul.api.common.error.AppException
import nl.brul.api.features.vote.dto.VoteRequest
import nl.brul.api.security.AuthenticatedUser

private val voteJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
    coerceInputValues = false
}

fun Route.voteRoutes(voteService: VoteService) {
    authenticate("auth-jwt") {

        put("/reports/{id}/vote") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw AppException(
                    HttpStatusCode.Unauthorized,
                    "UNAUTHORIZED",
                    "Authentication is required."
                )

            val reportId = parseReportId(call.parameters["id"])

            require(
                call.request.contentType()
                    .match(ContentType.Application.Json)
            ) {
                "Content-Type must be application/json."
            }

            val jsonElement = voteJson.parseToJsonElement(
                call.receiveText()
            )

            require(jsonElement is JsonObject) {
                "Request body must be a JSON object."
            }

            require(jsonElement.keys == setOf("value")) {
                "Only the value field is allowed."
            }

            val request = voteJson.decodeFromJsonElement<VoteRequest>(
                jsonElement
            )

            val response = voteService.vote(
                reportId = reportId,
                userId = user.user.id,
                value = request.value
            )

            call.respond(HttpStatusCode.OK, response)
        }

        delete("/reports/{id}/vote") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw AppException(
                    HttpStatusCode.Unauthorized,
                    "UNAUTHORIZED",
                    "Authentication is required."
                )

            val reportId = parseReportId(call.parameters["id"])

            voteService.removeVote(
                reportId = reportId,
                userId = user.user.id
            )

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun parseReportId(value: String?): Long {
    val id = value?.toLongOrNull()

    require(id != null && id > 0) {
        "Invalid report ID."
    }

    return id
}