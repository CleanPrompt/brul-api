// src/nl/brul/api/features/vote/repository/ExposedVoteRepository.kt

package nl.brul.api.features.vote.repository

import io.ktor.http.HttpStatusCode
import nl.brul.api.common.error.AppException
import nl.brul.api.database.tables.ReportsTable
import nl.brul.api.database.tables.VotesTable
import nl.brul.api.features.vote.dto.VoteResponse
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class ExposedVoteRepository : VoteRepository {

    override fun saveVote(
        reportId: Long,
        userId: Long,
        value: String
    ): VoteResponse = transaction {

        // Vergrendel de melding tijdens het stemmen.
        val report = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq reportId }
            .forUpdate()
            .singleOrNull()
            ?: throw reportNotFound()

        // Je mag niet op je eigen melding stemmen.
        if (report[ReportsTable.ownerId] == userId) {
            throw AppException(
                HttpStatusCode.Forbidden,
                "CANNOT_VOTE_OWN_REPORT",
                "You cannot vote on your own report."
            )
        }

        // Kijk of deze gebruiker al heeft gestemd.
        val existingVote = VotesTable
            .selectAll()
            .where {
                (VotesTable.reportId eq reportId) and
                        (VotesTable.userId eq userId)
            }
            .singleOrNull()

        val now = Instant.now()

        if (existingVote == null) {

            // Eerste stem van deze gebruiker.
            VotesTable.insert {
                it[VotesTable.reportId] = reportId
                it[VotesTable.userId] = userId
                it[VotesTable.value] = value
                it[createdAt] = now
                it[updatedAt] = null
            }

        } else if (existingVote[VotesTable.value] != value) {

            // Bestaande stem veranderen.
            VotesTable.update({
                (VotesTable.reportId eq reportId) and
                        (VotesTable.userId eq userId)
            }) {
                it[VotesTable.value] = value
                it[updatedAt] = now
            }
        }

        // Bereken de actuele stemtotalen.
        val votes = VotesTable
            .selectAll()
            .where { VotesTable.reportId eq reportId }
            .toList()

        VoteResponse(
            reportId = reportId,
            myVote = value,
            likeCount = votes.count {
                it[VotesTable.value] == "LIKE"
            }.toLong(),
            dislikeCount = votes.count {
                it[VotesTable.value] == "DISLIKE"
            }.toLong()
        )
    }

    override fun deleteVote(
        reportId: Long,
        userId: Long
    ) {
        transaction {

            // Controleer of de melding bestaat.
            val report = ReportsTable
                .selectAll()
                .where { ReportsTable.id eq reportId }
                .forUpdate()
                .singleOrNull()
                ?: throw reportNotFound()

            // Verwijder uitsluitend de stem van deze gebruiker.
            // Als er geen stem bestaat, gebeurt er niets.
            VotesTable.deleteWhere {
                (VotesTable.reportId eq report[ReportsTable.id]) and
                        (VotesTable.userId eq userId)
            }
        }
    }

    private fun reportNotFound() = AppException(
        HttpStatusCode.NotFound,
        "REPORT_NOT_FOUND",
        "The report was not found."
    )
}