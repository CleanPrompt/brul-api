package nl.brul.api.features.auth.repository

import java.time.Instant

interface RefreshSessionRepository {

    fun create(
        userId: Long,
        tokenHash: String,
        now: Instant,
        expiresAt: Instant
    )
}