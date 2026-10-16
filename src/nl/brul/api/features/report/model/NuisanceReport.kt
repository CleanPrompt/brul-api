package nl.brul.api.features.report.model

import java.time.Instant

class NuisanceReport(
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
    override val type = ReportType.NUISANCE

    init {
        validateCategory()
    }

    override fun validateCategory() {
        require(
            category in setOf(
                ReportCategory.NOISE,
                ReportCategory.WASTE,
                ReportCategory.VANDALISM,
                ReportCategory.PARKING
            )
        ) {
            "Invalid nuisance report category."
        }
    }
}