// src/nl/brul/api/database/DatabaseFactory.kt

package nl.brul.api.database

import nl.brul.api.database.tables.UsersTable
import nl.brul.api.database.tables.ReportsTable
import nl.brul.api.database.tables.VotesTable
import nl.brul.api.database.tables.HelpParticipationsTable
import nl.brul.api.database.tables.RefreshSessionsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun init(databasePath: String) {
        require(databasePath.isNotBlank()) {
            "Database path must not be blank."
        }

        val databaseFile = File(databasePath).absoluteFile
        val databaseDirectory = databaseFile.parentFile

        require(databaseDirectory.isDirectory || databaseDirectory.mkdirs()) {
            "Could not create database directory: ${databaseDirectory.path}"
        }

        Database.connect(
            url = "jdbc:h2:file:$databasePath;DB_CLOSE_ON_EXIT=FALSE",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction {
            SchemaUtils.create(
                UsersTable,
                ReportsTable,
                VotesTable,
                HelpParticipationsTable,
                RefreshSessionsTable
            )
        }

        println("Brul database initialized: $databasePath")
    }
}