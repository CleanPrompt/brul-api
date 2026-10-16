package nl.brul.api.features.report.model

import java.time.Instant

abstract class Report(
    val id: Long,
    val ownerId: Long,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val location: Location,
    val imageUrl: String?,
    val viewCount: Long,
    val createdAt: Instant,
    val updatedAt: Instant?
) {
    abstract val type: ReportType

    abstract fun validateCategory()

    init {
        require(id > 0)
        require(ownerId > 0)
        require(title.isNotBlank())
        require(description.isNotBlank())
        require(viewCount >= 0)
    }
}