package nl.brul.api.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 320).uniqueIndex()
    val passwordHash = varchar("password_hash", 60)
    val role = varchar("role", 10)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}