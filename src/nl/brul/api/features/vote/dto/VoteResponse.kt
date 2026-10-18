// src/nl/brul/api/features/vote/dto/VoteResponse.kt

package nl.brul.api.features.vote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VoteResponse(
    val reportId: Long,
    val myVote: String,
    val likeCount: Long,
    val dislikeCount: Long
)