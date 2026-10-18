
// src/nl/brul/api/features/help/dto/ParticipationResponse.kt
package nl.brul.api.features.help.dto

import kotlinx.serialization.Serializable

@Serializable
data class ParticipationResponse(
    val helpRequestId: Long,
    val userId: Long,
    val joinedAt: String,
    val participantCount: Int,
    val maxHelpers: Int,
    val status: String
)
