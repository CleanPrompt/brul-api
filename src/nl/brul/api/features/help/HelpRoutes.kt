// src/nl/brul/api/features/help/HelpRoutes.kt
package nl.brul.api.features.help

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import nl.brul.api.common.error.AppException
import nl.brul.api.common.validation.StrictJson
import nl.brul.api.security.AuthenticatedUser

fun Route.helpRoutes(helpService: HelpService) {

    get("/help-requests/{id}/participants") {
        val id = parseHelpId(call.parameters["id"])

        call.respond(
            HttpStatusCode.OK,
            helpService.findParticipants(id)
        )
    }

    authenticate("auth-jwt") {

        post("/help-requests/{id}/participants") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val id = parseHelpId(call.parameters["id"])

            StrictJson.requireNoBody(call)

            call.respond(
                HttpStatusCode.Created,
                helpService.join(id, user.user.id)
            )
        }

        delete("/help-requests/{id}/participants/me") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val id = parseHelpId(call.parameters["id"])

            StrictJson.requireNoBody(call)

            helpService.leave(id, user.user.id)

            call.respond(HttpStatusCode.NoContent)
        }

        patch("/help-requests/{id}/capacity") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val id = parseHelpId(call.parameters["id"])

            val body = StrictJson.readObject(
                call,
                setOf("maxHelpers")
            )

            require(body.keys == setOf("maxHelpers")) {
                "Only maxHelpers is allowed."
            }

            val value = body["maxHelpers"]

            require(
                value is JsonPrimitive && !value.isString
            ) {
                "maxHelpers must be an integer."
            }

            val maxHelpers = value.intOrNull
                ?: throw IllegalArgumentException(
                    "maxHelpers must be an integer."
                )

            call.respond(
                HttpStatusCode.OK,
                helpService.updateCapacity(
                    reportId = id,
                    ownerId = user.user.id,
                    maxHelpers = maxHelpers
                )
            )
        }

        post("/help-requests/{id}/close") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val id = parseHelpId(call.parameters["id"])

            StrictJson.requireNoBody(call)

            call.respond(
                HttpStatusCode.OK,
                helpService.close(id, user.user.id)
            )
        }
    }
}

private fun parseHelpId(value: String?): Long {
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
