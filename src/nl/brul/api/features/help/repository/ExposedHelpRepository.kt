
// src/nl/brul/api/features/help/repository/ExposedHelpRepository.kt
package nl.brul.api.features.help.repository

import io.ktor.http.HttpStatusCode
import nl.brul.api.common.error.AppException
import nl.brul.api.database.tables.HelpParticipationsTable
import nl.brul.api.database.tables.ReportsTable
import nl.brul.api.database.tables.VotesTable
import nl.brul.api.features.help.dto.ParticipantResponse
import nl.brul.api.features.help.dto.ParticipationResponse
import nl.brul.api.features.report.dto.ReportResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class ExposedHelpRepository : HelpRepository {

    override fun join(
        reportId: Long,
        userId: Long
    ): ParticipationResponse = transaction {
        val report = lockedHelpRequest(reportId)

        if (report[ReportsTable.ownerId] == userId) {
            fail(
                HttpStatusCode.Forbidden,
                "CANNOT_JOIN_OWN_REQUEST",
                "You cannot join your own help request."
            )
        }

        val participations = participationRows(reportId)

        val activeCount = participations.count {
            it[HelpParticipationsTable.leftAt] == null
        }

        val alreadyParticipating = participations.any {
            it[HelpParticipationsTable.userId] == userId &&
                    it[HelpParticipationsTable.leftAt] == null
        }

        if (alreadyParticipating) {
            fail(
                HttpStatusCode.Conflict,
                "ALREADY_PARTICIPATING",
                "You already participate in this help request."
            )
        }

        if (report[ReportsTable.closedAt] != null) {
            fail(
                HttpStatusCode.Conflict,
                "HELP_REQUEST_CLOSED",
                "This help request is closed."
            )
        }

        val maxHelpers = requireNotNull(
            report[ReportsTable.maxHelpers]
        )

        if (activeCount >= maxHelpers) {
            fail(
                HttpStatusCode.Conflict,
                "HELP_REQUEST_FULL",
                "This help request is full."
            )
        }

        val now = Instant.now()

        HelpParticipationsTable.insert {
            it[helpRequestId] = reportId
            it[HelpParticipationsTable.userId] = userId
            it[joinedAt] = now
            it[leftAt] = null
        }

        val newCount = activeCount + 1

        ParticipationResponse(
            helpRequestId = reportId,
            userId = userId,
            joinedAt = now.toString(),
            participantCount = newCount,
            maxHelpers = maxHelpers,
            status = if (newCount >= maxHelpers) {
                "FULL"
            } else {
                "OPEN"
            }
        )
    }

    override fun leave(
        reportId: Long,
        userId: Long
    ) {
        transaction {
            lockedHelpRequest(reportId)

            val participation = participationRows(reportId)
                .find {
                    it[HelpParticipationsTable.userId] == userId &&
                            it[HelpParticipationsTable.leftAt] == null
                }
                ?: fail(
                    HttpStatusCode.NotFound,
                    "PARTICIPATION_NOT_FOUND",
                    "You are not participating in this help request."
                )

            val participationId =
                participation[HelpParticipationsTable.id]

            HelpParticipationsTable.update({
                HelpParticipationsTable.id eq participationId
            }) {
                it[leftAt] = Instant.now()
            }
        }
    }

    override fun findParticipants(
        reportId: Long
    ): List<ParticipantResponse> = transaction {
        findHelpRequest(reportId)

        participationRows(reportId)
            .filter {
                it[HelpParticipationsTable.leftAt] == null
            }
            .sortedWith(
                compareBy<ResultRow> {
                    it[HelpParticipationsTable.joinedAt]
                }.thenBy {
                    it[HelpParticipationsTable.userId]
                }
            )
            .map {
                ParticipantResponse(
                    userId = it[HelpParticipationsTable.userId],
                    joinedAt = it[
                        HelpParticipationsTable.joinedAt
                    ].toString()
                )
            }
    }

    override fun updateCapacity(
        reportId: Long,
        ownerId: Long,
        maxHelpers: Int
    ): ReportResponse = transaction {
        val report = lockedHelpRequest(reportId)

        checkOwner(report, ownerId)

        val activeCount = participationRows(reportId).count {
            it[HelpParticipationsTable.leftAt] == null
        }

        if (maxHelpers < activeCount) {
            fail(
                HttpStatusCode.Conflict,
                "CAPACITY_BELOW_PARTICIPANTS",
                "Capacity cannot be below the number of participants."
            )
        }

        if (report[ReportsTable.maxHelpers] != maxHelpers) {
            ReportsTable.update({
                ReportsTable.id eq reportId
            }) {
                it[ReportsTable.maxHelpers] = maxHelpers
                it[updatedAt] = Instant.now()
            }
        }

        reportResponse(reportId)
    }

    override fun close(
        reportId: Long,
        ownerId: Long
    ): ReportResponse = transaction {
        val report = lockedHelpRequest(reportId)

        checkOwner(report, ownerId)

        if (report[ReportsTable.closedAt] == null) {
            val now = Instant.now()

            ReportsTable.update({
                ReportsTable.id eq reportId
            }) {
                it[closedAt] = now
                it[updatedAt] = now
            }
        }

        reportResponse(reportId)
    }

    private fun findHelpRequest(reportId: Long): ResultRow {
        val report = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq reportId }
            .singleOrNull()
            ?: fail(
                HttpStatusCode.NotFound,
                "REPORT_NOT_FOUND",
                "The report was not found."
            )

        if (report[ReportsTable.type] != "HELP_REQUEST") {
            fail(
                HttpStatusCode.BadRequest,
                "NOT_A_HELP_REQUEST",
                "This report is not a help request."
            )
        }

        return report
    }

    private fun lockedHelpRequest(reportId: Long): ResultRow {
        val report = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq reportId }
            .forUpdate()
            .singleOrNull()
            ?: fail(
                HttpStatusCode.NotFound,
                "REPORT_NOT_FOUND",
                "The report was not found."
            )

        if (report[ReportsTable.type] != "HELP_REQUEST") {
            fail(
                HttpStatusCode.BadRequest,
                "NOT_A_HELP_REQUEST",
                "This report is not a help request."
            )
        }

        return report
    }

    private fun participationRows(
        reportId: Long
    ): List<ResultRow> {
        return HelpParticipationsTable
            .selectAll()
            .where {
                HelpParticipationsTable.helpRequestId eq reportId
            }
            .toList()
    }

    private fun checkOwner(
        report: ResultRow,
        ownerId: Long
    ) {
        if (report[ReportsTable.ownerId] != ownerId) {
            fail(
                HttpStatusCode.Forbidden,
                "FORBIDDEN",
                "Only the owner can change this help request."
            )
        }
    }

    private fun reportResponse(reportId: Long): ReportResponse {
        val report = findHelpRequest(reportId)

        val activeCount = participationRows(reportId).count {
            it[HelpParticipationsTable.leftAt] == null
        }

        val maxHelpers = requireNotNull(
            report[ReportsTable.maxHelpers]
        )

        val closedAt = report[ReportsTable.closedAt]

        val status = when {
            closedAt != null -> "CLOSED"
            activeCount >= maxHelpers -> "FULL"
            else -> "OPEN"
        }

        val votes = VotesTable
            .selectAll()
            .where { VotesTable.reportId eq reportId }
            .toList()

        return ReportResponse(
            id = report[ReportsTable.id],
            ownerId = report[ReportsTable.ownerId],
            type = report[ReportsTable.type],
            category = report[ReportsTable.category],
            title = report[ReportsTable.title],
            description = report[ReportsTable.description],
            latitude = report[ReportsTable.latitude],
            longitude = report[ReportsTable.longitude],
            imageUrl = report[ReportsTable.imageUrl],
            viewCount = report[ReportsTable.viewCount],
            likeCount = votes.count {
                it[VotesTable.value] == "LIKE"
            }.toLong(),
            dislikeCount = votes.count {
                it[VotesTable.value] == "DISLIKE"
            }.toLong(),
            maxHelpers = maxHelpers,
            participantCount = activeCount,
            status = status,
            closedAt = closedAt?.toString(),
            distanceKm = null,
            createdAt = report[
                ReportsTable.createdAt
            ].toString(),
            updatedAt = report[
                ReportsTable.updatedAt
            ]?.toString()
        )
    }

    private fun fail(
        status: HttpStatusCode,
        code: String,
        message: String
    ): Nothing {
        throw AppException(status, code, message)
    }
}
