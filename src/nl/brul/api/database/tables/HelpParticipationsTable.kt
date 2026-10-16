package nl.brul.api.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object HelpParticipationsTable : Table("help_participations") {
    val id = long("id").autoIncrement()

    val helpRequestId = reference(
        "help_request_id",
        ReportsTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val userId = reference(
        "user_id",
        UsersTable.id
    )

    val joinedAt = timestamp("joined_at")
    val leftAt = timestamp("left_at").nullable()

    override val primaryKey = PrimaryKey(id)
}