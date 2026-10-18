// nl/brul/api/features/auth/repository/ExposedRefreshSessionRepository.kt
package nl.brul.api.features.auth.repository

import nl.brul.api.database.tables.RefreshSessionsTable
import nl.brul.api.database.tables.UsersTable
import nl.brul.api.features.user.model.Role
import nl.brul.api.features.user.model.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
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

    override fun issueAccessTokenIfValid(
        tokenHash: String,
        now: Instant,
        issueToken: (User) -> String
    ): String? {
        return transaction {
            val session = RefreshSessionsTable
                .selectAll()
                .where { RefreshSessionsTable.tokenHash eq tokenHash }
                .forUpdate()
                .singleOrNull()
                ?: return@transaction null

            if (session[RefreshSessionsTable.expiresAt] <= now) {
                return@transaction null
            }

            if (session[RefreshSessionsTable.revokedAt] != null) {
                return@transaction null
            }

            val userId = session[RefreshSessionsTable.userId]

            val userRow = UsersTable
                .selectAll()
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?: return@transaction null

            val user = User(
                id = userRow[UsersTable.id],
                email = userRow[UsersTable.email],
                passwordHash = userRow[UsersTable.passwordHash],
                role = Role.valueOf(userRow[UsersTable.role]),
                createdAt = userRow[UsersTable.createdAt]
            )

            issueToken(user)
        }
    }

    override fun revokeIfValid(
        tokenHash: String,
        now: Instant
    ): Boolean {
        return transaction {
            val session = RefreshSessionsTable
                .selectAll()
                .where { RefreshSessionsTable.tokenHash eq tokenHash }
                .forUpdate()
                .singleOrNull()
                ?: return@transaction false

            if (session[RefreshSessionsTable.expiresAt] <= now) {
                return@transaction false
            }

            if (session[RefreshSessionsTable.revokedAt] != null) {
                return@transaction false
            }

            val sessionId = session[RefreshSessionsTable.id]

            RefreshSessionsTable.update(
                where = { RefreshSessionsTable.id eq sessionId }
            ) {
                it[revokedAt] = now
            }

            true
        }
    }
}