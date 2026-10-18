
// test/nl/brul/api/integration/HelpDatabaseIntegrationTest.kt
package nl.brul.api.integration

import nl.brul.api.database.tables.HelpParticipationsTable
import nl.brul.api.database.tables.ReportsTable
import nl.brul.api.database.tables.UsersTable
import nl.brul.api.database.tables.VotesTable
import nl.brul.api.features.help.repository.ExposedHelpRepository
import nl.brul.api.features.report.repository.ExposedReportRepository
import nl.brul.api.features.vote.repository.ExposedVoteRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HelpDatabaseIntegrationTest {

    private val helpRepository = ExposedHelpRepository()
    private val reportRepository = ExposedReportRepository()
    private val voteRepository = ExposedVoteRepository()

    private var ownerId = 0L
    private var helperId = 0L
    private var secondHelperId = 0L
    private var reportId = 0L

    @BeforeTest
    fun setUp() {
        val databaseName = "brul_test_${UUID.randomUUID()}"

        val database = Database.connect(
            url = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )

        transaction(database) {
            SchemaUtils.create(
                UsersTable,
                ReportsTable,
                VotesTable,
                HelpParticipationsTable
            )

            ownerId = createUser("owner@test.local")
            helperId = createUser("helper@test.local")
            secondHelperId = createUser("helper2@test.local")

            reportId = ReportsTable.insert {
                it[ownerId] = this@HelpDatabaseIntegrationTest.ownerId
                it[type] = "HELP_REQUEST"
                it[category] = "GROCERIES"
                it[title] = "Help with groceries"
                it[description] = "I need help with shopping."
                it[latitude] = 52.001
                it[longitude] = 4.213
                it[imageUrl] = null
                it[viewCount] = 0L
                it[createdAt] = Instant.parse(
                    "2026-10-18T10:00:00Z"
                )
                it[updatedAt] = null
                it[maxHelpers] = 1
                it[closedAt] = null
            }[ReportsTable.id]
        }
    }

    private fun createUser(emailAddress: String): Long {
        return UsersTable.insert {
            it[email] = emailAddress
            it[passwordHash] = "integration-test-placeholder"
            it[role] = "USER"
            it[createdAt] = Instant.parse(
                "2026-10-18T10:00:00Z"
            )
        }[UsersTable.id]
    }

    @Test
    fun `joining creates an active participation in the database`() {
        val result = helpRepository.join(reportId, helperId)

        val rows = transaction {
            HelpParticipationsTable.selectAll()
                .where {
                    HelpParticipationsTable.helpRequestId eq reportId
                }
                .toList()
        }

        assertEquals(1, rows.size)
        assertEquals(helperId, rows.single()[HelpParticipationsTable.userId])
        assertEquals(null, rows.single()[HelpParticipationsTable.leftAt])
        assertEquals(1, result.participantCount)
        assertEquals("FULL", result.status)
    }

    @Test
    fun `leaving and rejoining preserves participation history`() {
        val firstJoin = helpRepository.join(reportId, helperId)

        helpRepository.leave(reportId, helperId)

        val secondJoin = helpRepository.join(reportId, helperId)

        val rows = transaction {
            HelpParticipationsTable.selectAll()
                .where {
                    HelpParticipationsTable.helpRequestId eq reportId
                }
                .orderBy(HelpParticipationsTable.id)
                .toList()
        }

        assertEquals(2, rows.size)

        assertNotEquals(
            rows[0][HelpParticipationsTable.id],
            rows[1][HelpParticipationsTable.id]
        )

        assertTrue(
            rows[0][HelpParticipationsTable.leftAt] != null
        )

        assertEquals(
            null,
            rows[1][HelpParticipationsTable.leftAt]
        )

        assertEquals(1, secondJoin.participantCount)
        assertEquals("FULL", secondJoin.status)

        assertTrue(firstJoin.joinedAt.isNotBlank())
        assertTrue(secondJoin.joinedAt.isNotBlank())
    }

    @Test
    fun `leaving a full help request makes it open again`() {
        helpRepository.join(reportId, helperId)

        helpRepository.leave(reportId, helperId)

        val participants = helpRepository.findParticipants(reportId)

        val report = transaction {
            ReportsTable.selectAll()
                .where { ReportsTable.id eq reportId }
                .single()
        }

        assertTrue(participants.isEmpty())
        assertEquals(1, report[ReportsTable.maxHelpers])
        assertEquals(null, report[ReportsTable.closedAt])

        val nextJoin = helpRepository.join(
            reportId,
            secondHelperId
        )

        assertEquals(1, nextJoin.participantCount)
        assertEquals("FULL", nextJoin.status)
    }

    @Test
    fun `deleting report also deletes participation history`() {
        helpRepository.join(reportId, helperId)
        helpRepository.leave(reportId, helperId)
        helpRepository.join(reportId, helperId)

        val deleted = reportRepository.delete(
            id = reportId,
            userId = ownerId,
            isAdmin = false
        )

        val remainingReports = transaction {
            ReportsTable.selectAll()
                .where { ReportsTable.id eq reportId }
                .count()
        }

        val remainingParticipations = transaction {
            HelpParticipationsTable.selectAll()
                .where {
                    HelpParticipationsTable.helpRequestId eq reportId
                }
                .count()
        }

        assertTrue(deleted)
        assertEquals(0L, remainingReports)
        assertEquals(0L, remainingParticipations)
    }
}
