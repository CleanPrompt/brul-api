package nl.brul.api.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object VotesTable : Table("votes") {
    val userId = reference(
        "user_id",
        UsersTable.id
    )

    val reportId = reference(
        "report_id",
        ReportsTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val value = varchar("value", 10)

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(userId, reportId)
}