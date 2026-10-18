// nl/brul/api/features/report/model/HelpParticipation.kt
package nl.brul.api.features.report.model

import java.time.Instant

data class HelpParticipation(
    val id: Long,
    val helpRequestId: Long,
    val userId: Long,
    val joinedAt: Instant,
    val leftAt: Instant?
) {
    val isActive: Boolean
        get() = leftAt == null
}