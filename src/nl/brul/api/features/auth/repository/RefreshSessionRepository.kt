// src/nl/brul/api/features/auth/repository/RefreshSessionRepository.kt
package nl.brul.api.features.auth.repository

import nl.brul.api.features.user.model.User
import java.time.Instant

interface RefreshSessionRepository {

    /**
     * Creates a refresh session using a hashed refresh token.
     */
    fun create(
        userId: Long,
        tokenHash: String,
        now: Instant,
        expiresAt: Instant
    )

    /**
     * Issues an access token only when the refresh session is valid.
     *
     * The session is locked during validation and token creation.
     * Returns null when the session is missing, expired or revoked.
     */
    fun issueAccessTokenIfValid(
        tokenHash: String,
        now: Instant,
        issueToken: (User) -> String
    ): String?

    /**
     * Revokes a valid refresh session.
     *
     * Returns false when the session is missing, expired or
     * already revoked.
     */
    fun revokeIfValid(
        tokenHash: String,
        now: Instant
    ): Boolean
}