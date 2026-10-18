// src/nl/brul/api/features/vote/repository/VoteRepository.kt

package nl.brul.api.features.vote.repository

import nl.brul.api.features.vote.dto.VoteResponse

interface VoteRepository {

    fun saveVote(
        reportId: Long,
        userId: Long,
        value: String
    ): VoteResponse

    fun deleteVote(
        reportId: Long,
        userId: Long
    )
}