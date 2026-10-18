// src/nl/brul/api/features/report/dto/CreateReportRequest.kt

package nl.brul.api.features.report.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateReportRequest(
    val type: String,
    val category: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val maxHelpers: Int? = null
)