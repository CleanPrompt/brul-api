// src/nl/brul/api/features/vote/dto/VoteRequest.kt
package nl.brul.api.features.vote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VoteRequest(
    val value: String
)