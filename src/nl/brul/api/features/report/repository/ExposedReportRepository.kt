// src/nl/brul/api/features/report/repository/ExposedReportRepository.kt

package nl.brul.api.features.report.repository

import io.ktor.http.HttpStatusCode
import nl.brul.api.common.error.AppException
import nl.brul.api.database.tables.HelpParticipationsTable
import nl.brul.api.database.tables.ReportsTable
import nl.brul.api.database.tables.VotesTable
import nl.brul.api.features.report.model.HelpParticipation
import nl.brul.api.features.report.model.HelpRequest
import nl.brul.api.features.report.model.Location
import nl.brul.api.features.report.model.NuisanceReport
import nl.brul.api.features.report.model.Report
import nl.brul.api.features.report.model.ReportCategory
import nl.brul.api.features.report.model.ReportType
import nl.brul.api.features.report.model.SafetyReport
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class ExposedReportRepository : ReportRepository {

    override fun create(report: NewReport): Report = transaction {
        val now = Instant.now()

        val id = ReportsTable.insert {
            it[ownerId] = report.ownerId
            it[type] = report.type.name
            it[category] = report.category.name
            it[title] = report.title
            it[description] = report.description
            it[latitude] = report.location.latitude
            it[longitude] = report.location.longitude
            it[imageUrl] = report.imageUrl
            it[viewCount] = 0L
            it[createdAt] = now
            it[updatedAt] = null
            it[maxHelpers] = report.maxHelpers
            it[closedAt] = null
        } get ReportsTable.id

        loadReport(id)!!.report
    }

    override fun findAll(): List<ReportWithCounts> = transaction {
        ReportsTable.selectAll().map { row ->
            val report = row.toReport()
            withCounts(report)
        }
    }

    override fun findByIdAndIncreaseViews(
        id: Long
    ): ReportWithCounts? = transaction {
        val row = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq id }
            .forUpdate()
            .singleOrNull()
            ?: return@transaction null

        val newViewCount = row[ReportsTable.viewCount] + 1

        ReportsTable.update({ ReportsTable.id eq id }) {
            it[viewCount] = newViewCount
        }

        loadReport(id)
    }

    override fun update(
        id: Long,
        ownerId: Long,
        change: (Report) -> NewReport
    ): ReportWithCounts? = transaction {
        val row = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq id }
            .forUpdate()
            .singleOrNull()
            ?: return@transaction null

        val existing = row.toReport()

        if (existing.ownerId != ownerId) {
            throw AppException(
                HttpStatusCode.Forbidden,
                "FORBIDDEN",
                "You are not allowed to change this report."
            )
        }

        val newReport = change(existing)

        require(newReport.ownerId == existing.ownerId)
        require(newReport.type == existing.type)
        require(newReport.maxHelpers == (existing as? HelpRequest)?.maxHelpers)

        val changed =
            newReport.title != existing.title ||
                    newReport.description != existing.description ||
                    newReport.category != existing.category ||
                    newReport.location != existing.location ||
                    newReport.imageUrl != existing.imageUrl

        if (changed) {
            ReportsTable.update({ ReportsTable.id eq id }) {
                it[title] = newReport.title
                it[description] = newReport.description
                it[category] = newReport.category.name
                it[latitude] = newReport.location.latitude
                it[longitude] = newReport.location.longitude
                it[imageUrl] = newReport.imageUrl
                it[updatedAt] = Instant.now()
            }
        }

        loadReport(id)
    }

    override fun delete(
        id: Long,
        userId: Long,
        isAdmin: Boolean
    ): Boolean = transaction {
        val row = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq id }
            .forUpdate()
            .singleOrNull()
            ?: return@transaction false

        if (row[ReportsTable.ownerId] != userId && !isAdmin) {
            throw AppException(
                HttpStatusCode.Forbidden,
                "FORBIDDEN",
                "You are not allowed to delete this report."
            )
        }

        // Votes en deelnames hebben ON DELETE CASCADE.
        ReportsTable.deleteWhere {
            ReportsTable.id eq id
        }

        true
    }

    private fun loadReport(id: Long): ReportWithCounts? {
        val row = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq id }
            .singleOrNull()
            ?: return null

        return withCounts(row.toReport())
    }

    private fun withCounts(report: Report): ReportWithCounts {
        val votes = VotesTable
            .selectAll()
            .where { VotesTable.reportId eq report.id }
            .toList()

        return ReportWithCounts(
            report = report,
            likeCount = votes.count {
                it[VotesTable.value] == "LIKE"
            }.toLong(),
            dislikeCount = votes.count {
                it[VotesTable.value] == "DISLIKE"
            }.toLong()
        )
    }

    private fun ResultRow.toReport(): Report {
        val id = this[ReportsTable.id]
        val type = ReportType.valueOf(this[ReportsTable.type])

        val location = Location(
            latitude = this[ReportsTable.latitude],
            longitude = this[ReportsTable.longitude]
        )

        val ownerId = this[ReportsTable.ownerId]
        val category = ReportCategory.valueOf(this[ReportsTable.category])
        val title = this[ReportsTable.title]
        val description = this[ReportsTable.description]
        val imageUrl = this[ReportsTable.imageUrl]
        val viewCount = this[ReportsTable.viewCount]
        val createdAt = this[ReportsTable.createdAt]
        val updatedAt = this[ReportsTable.updatedAt]

        return when (type) {
            ReportType.SAFETY -> SafetyReport(
                id = id,
                ownerId = ownerId,
                title = title,
                description = description,
                category = category,
                location = location,
                imageUrl = imageUrl,
                viewCount = viewCount,
                createdAt = createdAt,
                updatedAt = updatedAt
            )

            ReportType.NUISANCE -> NuisanceReport(
                id = id,
                ownerId = ownerId,
                title = title,
                description = description,
                category = category,
                location = location,
                imageUrl = imageUrl,
                viewCount = viewCount,
                createdAt = createdAt,
                updatedAt = updatedAt
            )

            ReportType.HELP_REQUEST -> {
                val participations = HelpParticipationsTable
                    .selectAll()
                    .where {
                        HelpParticipationsTable.helpRequestId eq id
                    }
                    .map { participation ->
                        HelpParticipation(
                            id = participation[HelpParticipationsTable.id],
                            helpRequestId = participation[
                                HelpParticipationsTable.helpRequestId
                            ],
                            userId = participation[
                                HelpParticipationsTable.userId
                            ],
                            joinedAt = participation[
                                HelpParticipationsTable.joinedAt
                            ],
                            leftAt = participation[
                                HelpParticipationsTable.leftAt
                            ]
                        )
                    }

                HelpRequest(
                    id = id,
                    ownerId = ownerId,
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    imageUrl = imageUrl,
                    viewCount = viewCount,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    maxHelpers = requireNotNull(
                        this[ReportsTable.maxHelpers]
                    ),
                    closedAt = this[ReportsTable.closedAt],
                    participations = participations
                )
            }
        }
    }
}