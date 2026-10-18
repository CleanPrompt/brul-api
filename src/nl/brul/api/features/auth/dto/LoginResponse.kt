// // nl/brul/api/features/auth/dto/LoginResponse.kt
package nl.brul.api.features.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int = 900
)