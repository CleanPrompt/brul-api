package nl.brul.api.features.report.model

import kotlin.math.*

data class Location(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90."
        }

        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180."
        }
    }

    fun distanceTo(other: Location): Double {
        val earthRadiusKm = 6371.0

        val latitudeDifference =
            Math.toRadians(other.latitude - latitude)

        val longitudeDifference =
            Math.toRadians(other.longitude - longitude)

        val a =
            sin(latitudeDifference / 2).pow(2) +
                    cos(Math.toRadians(latitude)) *
                    cos(Math.toRadians(other.latitude)) *
                    sin(longitudeDifference / 2).pow(2)

        val safeA = a.coerceIn(0.0, 1.0)

        return 2 * earthRadiusKm * asin(sqrt(safeA))
    }
}