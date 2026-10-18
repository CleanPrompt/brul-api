// src/nl/brul/api/features/report/dto/ReportResponse.kt

package nl.brul.api.features.report.dto

import kotlinx.serialization.Serializable
import nl.brul.api.features.report.model.HelpRequest
import nl.brul.api.features.report.model.Report

@Serializable
data class ReportResponse(
    val id: Long,
    val ownerId: Long,
    val type: String,
    val category: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val viewCount: Long,
    val likeCount: Long,
    val dislikeCount: Long,
    val maxHelpers: Int?,
    val participantCount: Int?,
    val status: String?,
    val closedAt: String?,
    val distanceKm: Double?,
    val createdAt: String,
    val updatedAt: String?
) {
    companion object {
        fun from(
            report: Report,
            likeCount: Long = 0,
            dislikeCount: Long = 0,
            distanceKm: Double? = null
        ): ReportResponse {
            val helpRequest = report as? HelpRequest

            return ReportResponse(
                id = report.id,
                ownerId = report.ownerId,
                type = report.type.name,
                category = report.category.name,
                title = report.title,
                description = report.description,
                latitude = report.location.latitude,
                longitude = report.location.longitude,
                imageUrl = report.imageUrl,
                viewCount = report.viewCount,
                likeCount = likeCount,
                dislikeCount = dislikeCount,
                maxHelpers = helpRequest?.maxHelpers,
                participantCount = helpRequest?.participantCount,
                status = helpRequest?.status?.name,
                closedAt = helpRequest?.closedAt?.toString(),
                distanceKm = distanceKm,
                createdAt = report.createdAt.toString(),
                updatedAt = report.updatedAt?.toString()
            )
        }
    }
}