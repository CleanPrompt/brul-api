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

        // Lock the report while processing the vote.
        val report = ReportsTable
            .selectAll()
            .where { ReportsTable.id eq reportId }
            .forUpdate()
            .singleOrNull()
            ?: throw reportNotFound()

        // Users cannot vote on their own reports.
        if (report[ReportsTable.ownerId] == userId) {
            throw AppException(
                HttpStatusCode.Forbidden,
                "CANNOT_VOTE_OWN_REPORT",
                "You cannot vote on your own report."
            )
        }

        // Check whether this user has already voted.
        val existingVote = VotesTable
            .selectAll()
            .where {
                (VotesTable.reportId eq reportId) and
                        (VotesTable.userId eq userId)
            }
            .singleOrNull()

        val now = Instant.now()

        if (existingVote == null) {

            // Save the user's first vote.
            VotesTable.insert {
                it[VotesTable.reportId] = reportId
                it[VotesTable.userId] = userId
                it[VotesTable.value] = value
                it[createdAt] = now
                it[updatedAt] = null
            }

        } else if (existingVote[VotesTable.value] != value) {

            // Update the user's existing vote.
            VotesTable.update({
                (VotesTable.reportId eq reportId) and
                        (VotesTable.userId eq userId)
            }) {
                it[VotesTable.value] = value
                it[updatedAt] = now
            }
        }

        // Calculate the current vote totals.
        val votes = VotesTable
            .selectAll()
            .where { VotesTable.reportId eq reportId }
            .toList()

        return@transaction VoteResponse(
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

            // Check whether the report exists.
            val report = ReportsTable
                .selectAll()
                .where { ReportsTable.id eq reportId }
                .forUpdate()
                .singleOrNull()
                ?: throw reportNotFound()

            // Delete only this user's vote.
            // If no vote exists, nothing happens.
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
