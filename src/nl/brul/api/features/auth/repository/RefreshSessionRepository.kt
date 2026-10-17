package nl.brul.api.features.auth.repository

import nl.brul.api.features.user.model.User
import java.time.Instant

interface RefreshSessionRepository {

    fun create(
        userId: Long,
        tokenHash: String,
        now: Instant,
        expiresAt: Instant
    )

    fun issueAccessTokenIfValid(
        tokenHash: String,
        now: Instant,
        issueToken: (User) -> String
    ): String?

    fun revokeIfValid(
        tokenHash: String,
        now: Instant
    ): Boolean
}