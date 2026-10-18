// src/nl/brul/api/features/report/repository/ReportRepository.kt

package nl.brul.api.features.report.repository

import nl.brul.api.features.report.model.Location
import nl.brul.api.features.report.model.Report
import nl.brul.api.features.report.model.ReportCategory
import nl.brul.api.features.report.model.ReportType

data class NewReport(
    val ownerId: Long,
    val type: ReportType,
    val category: ReportCategory,
    val title: String,
    val description: String,
    val location: Location,
    val imageUrl: String?,
    val maxHelpers: Int?
)

data class ReportWithCounts(
    val report: Report,
    val likeCount: Long,
    val dislikeCount: Long
)

interface ReportRepository {
    fun create(report: NewReport): Report

    fun findAll(): List<ReportWithCounts>

    fun findByIdAndIncreaseViews(id: Long): ReportWithCounts?

    fun update(
        id: Long,
        ownerId: Long,
        change: (Report) -> NewReport
    ): ReportWithCounts?

    fun delete(
        id: Long,
        userId: Long,
        isAdmin: Boolean
    ): Boolean
}