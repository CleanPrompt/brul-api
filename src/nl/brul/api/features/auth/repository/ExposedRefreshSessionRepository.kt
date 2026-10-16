package nl.brul.api.features.auth.repository

import nl.brul.api.database.tables.RefreshSessionsTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ExposedRefreshSessionRepository : RefreshSessionRepository {

    override fun create(
        userId: Long,
        tokenHash: String,
        now: Instant,
        expiresAt: Instant
    ) {
        transaction {
            RefreshSessionsTable.insert {
                it[RefreshSessionsTable.userId] = userId
                it[RefreshSessionsTable.tokenHash] = tokenHash
                it[RefreshSessionsTable.createdAt] = now
                it[RefreshSessionsTable.expiresAt] = expiresAt
                it[RefreshSessionsTable.revokedAt] = null
            }
        }
    }
}