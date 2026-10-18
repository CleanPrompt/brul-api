// src/nl/brul/api/features/help/repository/HelpRepository.kt
package nl.brul.api.features.help.repository

import nl.brul.api.features.help.dto.ParticipantResponse
import nl.brul.api.features.help.dto.ParticipationResponse
import nl.brul.api.features.report.dto.ReportResponse

interface HelpRepository {

    fun join(
        reportId: Long,
        userId: Long
    ): ParticipationResponse

    fun leave(
        reportId: Long,
        userId: Long
    )

    fun findParticipants(
        reportId: Long
    ): List<ParticipantResponse>

    fun updateCapacity(
        reportId: Long,
        ownerId: Long,
        maxHelpers: Int
    ): ReportResponse

    fun close(
        reportId: Long,
        ownerId: Long
    ): ReportResponse
}
