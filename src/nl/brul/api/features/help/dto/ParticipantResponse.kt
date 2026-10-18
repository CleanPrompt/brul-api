// src/nl/brul/api/features/help/dto/ParticipantResponse.kt
package nl.brul.api.features.help.dto

import kotlinx.serialization.Serializable

@Serializable
data class ParticipantResponse(
    val userId: Long,
    val joinedAt: String
)
