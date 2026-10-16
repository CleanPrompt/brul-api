package nl.brul.api.database

import nl.brul.api.database.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun init() {
        val databasePath = "./data/brul"

        File(databasePath).absoluteFile.parentFile?.mkdirs()

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