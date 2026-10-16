package nl.brul.api.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object RefreshSessionsTable : Table("refresh_sessions") {
    val id = long("id").autoIncrement()

    val userId = reference(
        "user_id",
        UsersTable.id
    )

    val tokenHash = varchar("token_hash", 64).uniqueIndex()

    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}