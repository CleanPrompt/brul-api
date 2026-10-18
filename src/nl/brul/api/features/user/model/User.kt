// nl/brul/api/features/user/model/User.kt
package nl.brul.api.features.user.model

import java.time.Instant

data class User(
    val id: Long,
    val email: String,
    val passwordHash: String,
    val role: Role,
    val createdAt: Instant
)