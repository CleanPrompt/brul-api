// src/nl/brul/api/features/report/ReportService.kt
package nl.brul.api.features.report

import io.ktor.http.HttpStatusCode
import nl.brul.api.common.error.AppException
import nl.brul.api.features.report.dto.CreateReportRequest
import nl.brul.api.features.report.dto.ReportResponse
import nl.brul.api.features.report.model.HelpRequest
import nl.brul.api.features.report.model.Location
import nl.brul.api.features.report.model.ReportCategory
import nl.brul.api.features.report.model.ReportType
import nl.brul.api.features.report.repository.NewReport
import nl.brul.api.features.report.repository.ReportRepository
import nl.brul.api.features.report.repository.ReportWithCounts
import java.net.URI

data class ReportSearch(
    val q: String?,
    val type: String?,
    val category: String?,
    val latitude: String?,
    val longitude: String?,
    val radiusKm: String?,
    val sort: String?
)

data class ReportChanges(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val fields: Set<String>
)

class ReportService(
    private val reports: ReportRepository
) {
    companion object {
        private const val MIN_TITLE_LENGTH = 5
        private const val MAX_TITLE_LENGTH = 120
        private const val MIN_DESCRIPTION_LENGTH = 10
        private const val MAX_DESCRIPTION_LENGTH = 2000
    }

    fun create(
        request: CreateReportRequest,
        ownerId: Long,
        maxHelpersProvided: Boolean
    ): ReportResponse {
        require(ownerId > 0)

        val type = parseType(request.type)
        val category = parseCategory(request.category)

        validateCategory(type, category)

        val title = validateTitle(request.title)
        val description = validateDescription(request.description)
        val location = Location(request.latitude, request.longitude)
        val imageUrl = validateImageUrl(request.imageUrl)

        if (type == ReportType.HELP_REQUEST) {
            require(
                request.maxHelpers != null &&
                        request.maxHelpers in 1..20
            ) {
                "Help requests require maxHelpers between 1 and 20."
            }
        } else {
            require(!maxHelpersProvided) {
                "maxHelpers is only allowed for help requests."
            }
        }

        val saved = reports.create(
            NewReport(
                ownerId = ownerId,
                type = type,
                category = category,
                title = title,
                description = description,
                location = location,
                imageUrl = imageUrl,
                maxHelpers = request.maxHelpers
            )
        )

        return ReportResponse.from(saved)
    }

    fun findAll(search: ReportSearch): List<ReportResponse> {
        val type = search.type?.let(::parseType)
        val category = search.category?.let(::parseCategory)

        if (type != null && category != null) {
            validateCategory(type, category)
        }

        val latitude = search.latitude
        val longitude = search.longitude

        require((latitude == null) == (longitude == null)) {
            "Latitude and longitude must be provided together."
        }

        val searchLocation = if (latitude != null && longitude != null) {
            val latitudeNumber = latitude.toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid latitude.")

            val longitudeNumber = longitude.toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid longitude.")

            Location(latitudeNumber, longitudeNumber)
        } else {
            null
        }

        val radius = search.radiusKm?.toDoubleOrNull()

        if (search.radiusKm != null) {
            require(
                radius != null &&
                        radius.isFinite() &&
                        radius > 0 &&
                        radius <= 25 &&
                        searchLocation != null
            ) {
                "Invalid radiusKm."
            }
        }

        val sort = search.sort ?: "newest"

        require(
            sort in setOf(
                "newest",
                "mostLiked",
                "mostViewed",
                "nearest"
            )
        ) {
            "Invalid sort value."
        }

        require(sort != "nearest" || searchLocation != null) {
            "Nearest sorting requires coordinates."
        }

        val query = search.q?.trim().orEmpty()

        val matching = reports.findAll().filter { item ->
            val report = item.report

            val matchesText =
                query.isEmpty() ||
                        report.title.contains(query, ignoreCase = true) ||
                        report.description.contains(query, ignoreCase = true)

            val matchesType = type == null || report.type == type

            val matchesCategory =
                category == null || report.category == category

            val matchesRadius = if (radius != null && searchLocation != null) {
                report.location.distanceTo(searchLocation) <= radius
            } else {
                true
            }

            matchesText &&
                    matchesType &&
                    matchesCategory &&
                    matchesRadius
        }

        val sorted = when (sort) {
            "newest" -> matching.sortedWith(
                compareByDescending<ReportWithCounts> {
                    it.report.createdAt
                }.thenByDescending {
                    it.report.id
                }
            )

            "mostLiked" -> matching.sortedWith(
                compareByDescending<ReportWithCounts> {
                    it.likeCount
                }.thenByDescending {
                    it.report.createdAt
                }.thenByDescending {
                    it.report.id
                }
            )

            "mostViewed" -> matching.sortedWith(
                compareByDescending<ReportWithCounts> {
                    it.report.viewCount
                }.thenByDescending {
                    it.report.createdAt
                }.thenByDescending {
                    it.report.id
                }
            )

            "nearest" -> {
                val location = requireNotNull(searchLocation) {
                    "Nearest sorting requires coordinates."
                }

                matching.sortedWith(
                    compareBy<ReportWithCounts> {
                        it.report.location.distanceTo(location)
                    }.thenByDescending {
                        it.report.createdAt
                    }.thenByDescending {
                        it.report.id
                    }
                )
            }

            else -> matching
        }

        return sorted.map { item ->
            ReportResponse.from(
                report = item.report,
                likeCount = item.likeCount,
                dislikeCount = item.dislikeCount,
                distanceKm = searchLocation?.let { location ->
                    item.report.location.distanceTo(location)
                }
            )
        }
    }

    fun findById(id: Long): ReportResponse {
        require(id > 0)

        val result = reports.findByIdAndIncreaseViews(id)
            ?: throw reportNotFound()

        return toResponse(result)
    }

    fun update(
        id: Long,
        ownerId: Long,
        changes: ReportChanges
    ): ReportResponse {
        require(id > 0)
        require(changes.fields.isNotEmpty())

        val result = reports.update(id, ownerId) { existing ->
            val title = if ("title" in changes.fields) {
                validateTitle(requireNotNull(changes.title))
            } else {
                existing.title
            }

            val description = if ("description" in changes.fields) {
                validateDescription(requireNotNull(changes.description))
            } else {
                existing.description
            }

            val category = if ("category" in changes.fields) {
                parseCategory(requireNotNull(changes.category))
            } else {
                existing.category
            }

            validateCategory(existing.type, category)

            val latitude = if ("latitude" in changes.fields) {
                requireNotNull(changes.latitude)
            } else {
                existing.location.latitude
            }

            val longitude = if ("longitude" in changes.fields) {
                requireNotNull(changes.longitude)
            } else {
                existing.location.longitude
            }

            val location = Location(latitude, longitude)

            val imageUrl = if ("imageUrl" in changes.fields) {
                validateImageUrl(changes.imageUrl)
            } else {
                existing.imageUrl
            }

            NewReport(
                ownerId = existing.ownerId,
                type = existing.type,
                category = category,
                title = title,
                description = description,
                location = location,
                imageUrl = imageUrl,
                maxHelpers = (existing as? HelpRequest)?.maxHelpers
            )
        } ?: throw reportNotFound()

        return toResponse(result)
    }

    fun delete(id: Long, userId: Long, isAdmin: Boolean) {
        require(id > 0)

        if (!reports.delete(id, userId, isAdmin)) {
            throw reportNotFound()
        }
    }

    private fun toResponse(result: ReportWithCounts): ReportResponse {
        return ReportResponse.from(
            report = result.report,
            likeCount = result.likeCount,
            dislikeCount = result.dislikeCount
        )
    }

    private fun parseType(value: String): ReportType {
        return ReportType.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid report type.")
    }

    private fun parseCategory(value: String): ReportCategory {
        return ReportCategory.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid report category.")
    }

    private fun validateTitle(value: String): String {
        val result = value.trim()

        require(
            result.codePointCount(0, result.length) in
                    MIN_TITLE_LENGTH..MAX_TITLE_LENGTH
        ) {
            "Title must contain 5 to 120 characters."
        }

        return result
    }

    private fun validateDescription(value: String): String {
        val result = value.trim()

        require(
            result.codePointCount(0, result.length) in
                    MIN_DESCRIPTION_LENGTH..MAX_DESCRIPTION_LENGTH
        ) {
            "Description must contain 10 to 2000 characters."
        }

        return result
    }

    private fun validateCategory(
        type: ReportType,
        category: ReportCategory
    ) {
        val allowed = when (type) {
            ReportType.SAFETY -> setOf(
                ReportCategory.THEFT,
                ReportCategory.SUSPICIOUS_SITUATION,
                ReportCategory.TRAFFIC_SAFETY
            )

            ReportType.NUISANCE -> setOf(
                ReportCategory.NOISE,
                ReportCategory.WASTE,
                ReportCategory.VANDALISM,
                ReportCategory.PARKING
            )

            ReportType.HELP_REQUEST -> setOf(
                ReportCategory.GROCERIES,
                ReportCategory.TRANSPORT,
                ReportCategory.MOVING,
                ReportCategory.CHORES,
                ReportCategory.OTHER
            )
        }

        require(category in allowed) {
            "Category does not belong to this report type."
        }
    }

    private fun validateImageUrl(value: String?): String? {
        if (value == null) return null

        val result = value.trim()

        require(result.isNotEmpty() && result.length <= 2048) {
            "Invalid image URL."
        }

        val uri = URI(result)

        require(
            uri.isAbsolute &&
                    uri.scheme?.lowercase() in setOf("http", "https") &&
                    !uri.host.isNullOrBlank() &&
                    uri.rawUserInfo == null
        ) {
            "Image URL must be a valid HTTP(S) URL."
        }

        return result
    }

    private fun reportNotFound() = AppException(
        HttpStatusCode.NotFound,
        "REPORT_NOT_FOUND",
        "The report was not found."
    )
}
