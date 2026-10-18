
// src/nl/brul/api/features/help/HelpService.kt
package nl.brul.api.features.help

import nl.brul.api.features.help.dto.ParticipantResponse
import nl.brul.api.features.help.dto.ParticipationResponse
import nl.brul.api.features.help.repository.HelpRepository
import nl.brul.api.features.report.dto.ReportResponse

class HelpService(
    private val helps: HelpRepository
) {

    fun join(
        reportId: Long,
        userId: Long
    ): ParticipationResponse {
        require(reportId > 0) { "Invalid report ID." }
        require(userId > 0) { "Invalid user ID." }

        return helps.join(reportId, userId)
    }

    fun leave(
        reportId: Long,
        userId: Long
    ) {
        require(reportId > 0) { "Invalid report ID." }
        require(userId > 0) { "Invalid user ID." }

        helps.leave(reportId, userId)
    }

    fun findParticipants(
        reportId: Long
    ): List<ParticipantResponse> {
        require(reportId > 0) { "Invalid report ID." }

        return helps.findParticipants(reportId)
    }

    fun updateCapacity(
        reportId: Long,
        ownerId: Long,
        maxHelpers: Int
    ): ReportResponse {
        require(reportId > 0) { "Invalid report ID." }
        require(ownerId > 0) { "Invalid user ID." }

        require(maxHelpers in 1..20) {
            "maxHelpers must be between 1 and 20."
        }

        return helps.updateCapacity(
            reportId,
            ownerId,
            maxHelpers
        )
    }

    fun close(
        reportId: Long,
        ownerId: Long
    ): ReportResponse {
        require(reportId > 0) { "Invalid report ID." }
        require(ownerId > 0) { "Invalid user ID." }

        return helps.close(reportId, ownerId)
    }
}
