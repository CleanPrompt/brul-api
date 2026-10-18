// src/nl/brul/api/features/report/ReportRoutes.kt

package nl.brul.api.features.report

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.decodeFromJsonElement
import nl.brul.api.common.error.AppException
import nl.brul.api.features.report.dto.CreateReportRequest
import nl.brul.api.features.user.model.Role
import nl.brul.api.security.AuthenticatedUser

private val requestJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
    coerceInputValues = false
}

private val createFields = setOf(
    "type",
    "category",
    "title",
    "description",
    "latitude",
    "longitude",
    "imageUrl",
    "maxHelpers"
)

private val updateFields = setOf(
    "title",
    "description",
    "category",
    "latitude",
    "longitude",
    "imageUrl"
)

private val searchFields = setOf(
    "q",
    "type",
    "category",
    "latitude",
    "longitude",
    "radiusKm",
    "sort"
)

fun Route.reportRoutes(reportService: ReportService) {

    // Publiek: meldingen zoeken.
    get("/reports") {
        val parameters = call.request.queryParameters

        require(parameters.names().all { it in searchFields }) {
            "Unknown query parameter."
        }

        require(parameters.names().all {
            parameters.getAll(it)?.size == 1
        }) {
            "Duplicate query parameter."
        }

        val search = ReportSearch(
            q = parameters["q"],
            type = parameters["type"],
            category = parameters["category"],
            latitude = parameters["latitude"],
            longitude = parameters["longitude"],
            radiusKm = parameters["radiusKm"],
            sort = parameters["sort"]
        )

        call.respond(
            HttpStatusCode.OK,
            reportService.findAll(search)
        )
    }

    // Publiek: één melding ophalen.
    get("/reports/{id}") {
        val id = reportId(call.parameters["id"])

        call.respond(
            HttpStatusCode.OK,
            reportService.findById(id)
        )
    }

    authenticate("auth-jwt") {

        // Beveiligd: nieuwe melding.
        post("/reports") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val body = readJsonObject(
                call.request.contentType(),
                call.receiveText(),
                createFields
            )

            val request = requestJson
                .decodeFromJsonElement<CreateReportRequest>(body)

            call.respond(
                HttpStatusCode.Created,
                reportService.create(
                    request = request,
                    ownerId = user.user.id,
                    maxHelpersProvided = "maxHelpers" in body
                )
            )
        }

        // Beveiligd: eigen melding wijzigen.
        patch("/reports/{id}") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val id = reportId(call.parameters["id"])

            val body = readJsonObject(
                call.request.contentType(),
                call.receiveText(),
                updateFields
            )

            require(body.isNotEmpty()) {
                "A PATCH request must contain at least one field."
            }

            val changes = ReportChanges(
                title = body.stringField("title"),
                description = body.stringField("description"),
                category = body.stringField("category"),
                latitude = body.numberField("latitude"),
                longitude = body.numberField("longitude"),
                imageUrl = body.stringField("imageUrl", nullable = true),
                fields = body.keys
            )

            call.respond(
                HttpStatusCode.OK,
                reportService.update(
                    id = id,
                    ownerId = user.user.id,
                    changes = changes
                )
            )
        }

        // Beveiligd: eigen melding verwijderen,
        // of verwijderen als ADMIN.
        delete("/reports/{id}") {
            val user = call.principal<AuthenticatedUser>()
                ?: throw unauthorized()

            val id = reportId(call.parameters["id"])

            reportService.delete(
                id = id,
                userId = user.user.id,
                isAdmin = user.user.role == Role.ADMIN
            )

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun reportId(value: String?): Long {
    val id = value?.toLongOrNull()

    require(id != null && id > 0) {
        "Invalid report ID."
    }

    return id
}

private fun readJsonObject(
    contentType: ContentType,
    text: String,
    allowedFields: Set<String>
): JsonObject {
    require(contentType.match(ContentType.Application.Json)) {
        "Content-Type must be application/json."
    }

    val element = requestJson.parseToJsonElement(text)

    require(element is JsonObject) {
        "Request body must be a JSON object."
    }

    require(element.keys.all { it in allowedFields }) {
        "The request contains unknown fields."
    }

    return element
}

private fun JsonObject.stringField(
    name: String,
    nullable: Boolean = false
): String? {
    if (name !in this) return null

    val value = this[name]

    if (value == JsonNull && nullable) return null

    require(value is JsonPrimitive && value.isString) {
        "$name must be a string."
    }

    return value.content
}

private fun JsonObject.numberField(name: String): Double? {
    if (name !in this) return null

    val value = this[name]

    require(value is JsonPrimitive && !value.isString) {
        "$name must be a number."
    }

    return value.double
}

private fun unauthorized() = AppException(
    HttpStatusCode.Unauthorized,
    "UNAUTHORIZED",
    "Authentication is required."
)