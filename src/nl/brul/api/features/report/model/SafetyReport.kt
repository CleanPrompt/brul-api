package nl.brul.api.features.report.model

import java.time.Instant

class SafetyReport(
    id: Long,
    ownerId: Long,
    title: String,
    description: String,
    category: ReportCategory,
    location: Location,
    imageUrl: String?,
    viewCount: Long,
    createdAt: Instant,
    updatedAt: Instant?
) : Report(
    id, ownerId, title, description, category,
    location, imageUrl, viewCount, createdAt, updatedAt
) {
    override val type = ReportType.SAFETY

    init {
        validateCategory()
    }

    override fun validateCategory() {
        require(
            category in setOf(
                ReportCategory.THEFT,
                ReportCategory.SUSPICIOUS_SITUATION,
                ReportCategory.TRAFFIC_SAFETY
            )
        ) {
            "Invalid safety report category."
        }
    }
}