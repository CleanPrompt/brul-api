package nl.brul.api.features.report.model

import java.time.Instant

class HelpRequest(
    id: Long,
    ownerId: Long,
    title: String,
    description: String,
    category: ReportCategory,
    location: Location,
    imageUrl: String?,
    viewCount: Long,
    createdAt: Instant,
    updatedAt: Instant?,
    val maxHelpers: Int,
    val closedAt: Instant?,
    val participations: List<HelpParticipation>
) : Report(
    id, ownerId, title, description, category,
    location, imageUrl, viewCount, createdAt, updatedAt
) {
    override val type = ReportType.HELP_REQUEST

    val participantCount: Int
        get() = participations.count { it.isActive }

    val status: HelpStatus
        get() = when {
            closedAt != null -> HelpStatus.CLOSED
            participantCount >= maxHelpers -> HelpStatus.FULL
            else -> HelpStatus.OPEN
        }

    init {
        validateCategory()
        require(maxHelpers in 1..20)
        require(participantCount <= maxHelpers)
    }

    override fun validateCategory() {
        require(
            category in setOf(
                ReportCategory.GROCERIES,
                ReportCategory.TRANSPORT,
                ReportCategory.MOVING,
                ReportCategory.CHORES,
                ReportCategory.OTHER
            )
        ) {
            "Invalid help request category."
        }
    }

    fun ensureCanJoin(userId: Long) {
        require(userId != ownerId) {
            "The owner cannot join their own request."
        }

        require(participations.none {
            it.userId == userId && it.isActive
        }) {
            "User already participates."
        }

        require(status == HelpStatus.OPEN) {
            "Help request is not open."
        }
    }

    fun ensureCapacityAllowed(newMaxHelpers: Int) {
        require(newMaxHelpers in 1..20) {
            "Capacity must be between 1 and 20."
        }

        require(newMaxHelpers >= participantCount) {
            "Capacity cannot be below active participant count."
        }
    }
}