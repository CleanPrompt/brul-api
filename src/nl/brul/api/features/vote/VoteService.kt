// src/nl/brul/api/features/vote/VoteService.kt

package nl.brul.api.features.vote

import nl.brul.api.features.vote.dto.VoteResponse
import nl.brul.api.features.vote.repository.VoteRepository

class VoteService(
    private val votes: VoteRepository
) {
    fun vote(
        reportId: Long,
        userId: Long,
        value: String
    ): VoteResponse {
        require(reportId > 0) {
            "Invalid report ID."
        }

        require(userId > 0) {
            "Invalid user ID."
        }

        require(value == "LIKE" || value == "DISLIKE") {
            "Vote value must be LIKE or DISLIKE."
        }

        return votes.saveVote(reportId, userId, value)
    }

    fun removeVote(
        reportId: Long,
        userId: Long
    ) {
        require(reportId > 0) {
            "Invalid report ID."
        }

        require(userId > 0) {
            "Invalid user ID."
        }

        votes.deleteVote(reportId, userId)
    }
}