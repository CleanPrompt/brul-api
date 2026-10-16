package nl.brul.api.features.user.dto

import kotlinx.serialization.Serializable
import nl.brul.api.features.user.model.User

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val role: String,
    val createdAt: String
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            email = user.email,
            role = user.role.name,
            createdAt = user.createdAt.toString()
        )
    }
}