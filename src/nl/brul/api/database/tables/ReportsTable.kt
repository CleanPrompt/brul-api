package nl.brul.api.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object ReportsTable : Table("reports") {
    val id = long("id").autoIncrement()

    val ownerId = reference(
        "owner_id",
        UsersTable.id
    )

    val type = varchar("type", 20)
    val category = varchar("category", 40)

    val title = varchar("title", 500)
    val description = text("description")

    val latitude = double("latitude")
    val longitude = double("longitude")

    val imageUrl = varchar("image_url", 2048).nullable()

    val viewCount = long("view_count").default(0)

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at").nullable()

    val maxHelpers = integer("max_helpers").nullable()
    val closedAt = timestamp("closed_at").nullable()

    override val primaryKey = PrimaryKey(id)
}