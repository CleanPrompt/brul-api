
// src/nl/brul/api/features/vote/VoteRoutes.kt
package nl.brul.api.features.vote

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import kotlinx.serialization.json.decodeFromJsonElement
import nl.brul.api.common.error.AppException
import nl.brul.api.common.validation.StrictJson
import nl.brul.api.features.vote.dto.VoteRequest
import nl.brul.api.security.AuthenticatedUser

fun Route.voteRoutes(voteService: VoteService) {
    authenticate("auth-jwt") {

        put("/reports/{id}/vote") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val reportId = parseReportId(
                call.parameters["id"]
            )

            val body = StrictJson.readObject(
                call,
                setOf("value")
            )

            require(body.keys == setOf("value")) {
                "Only the value field is allowed."
            }

            val request = StrictJson.json
                .decodeFromJsonElement<VoteRequest>(body)

            val response = voteService.vote(
                reportId = reportId,
                userId = user.user.id,
                value = request.value
            )

            call.respond(HttpStatusCode.OK, response)
        }

        delete("/reports/{id}/vote") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val reportId = parseReportId(
                call.parameters["id"]
            )

            StrictJson.requireNoBody(call)

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

private fun unauthorized() = AppException(
    HttpStatusCode.Unauthorized,
    "UNAUTHORIZED",
    "Authentication is required."
)
