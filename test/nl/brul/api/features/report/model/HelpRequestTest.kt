
// test/nl/brul/api/features/report/model/HelpRequestTest.kt
package nl.brul.api.features.report.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HelpRequestTest {

    private val createdAt = Instant.parse(
        "2026-10-18T10:00:00Z"
    )

    private fun participation(
        userId: Long,
        leftAt: Instant? = null
    ): HelpParticipation {
        return HelpParticipation(
            id = userId,
            helpRequestId = 1L,
            userId = userId,
            joinedAt = createdAt,
            leftAt = leftAt
        )
    }

    private fun helpRequest(
        maxHelpers: Int = 2,
        closedAt: Instant? = null,
        participations: List<HelpParticipation> = emptyList()
    ): HelpRequest {
        return HelpRequest(
            id = 1L,
            ownerId = 10L,
            title = "Help with groceries",
            description = "I need help with grocery shopping.",
            category = ReportCategory.GROCERIES,
            location = Location(52.001, 4.213),
            imageUrl = null,
            viewCount = 0L,
            createdAt = createdAt,
            updatedAt = null,
            maxHelpers = maxHelpers,
            closedAt = closedAt,
            participations = participations
        )
    }

    @Test
    fun `new help request has open status`() {
        val request = helpRequest()

        assertEquals(HelpStatus.OPEN, request.status)
        assertEquals(0, request.participantCount)
    }

    @Test
    fun `help request with available capacity remains open`() {
        val request = helpRequest(
            maxHelpers = 2,
            participations = listOf(participation(20L))
        )

        assertEquals(HelpStatus.OPEN, request.status)
        assertEquals(1, request.participantCount)
    }

    @Test
    fun `help request becomes full when capacity is reached`() {
        val request = helpRequest(
            maxHelpers = 1,
            participations = listOf(participation(20L))
        )

        assertEquals(HelpStatus.FULL, request.status)
    }

    @Test
    fun `closed help request remains closed even when full`() {
        val request = helpRequest(
            maxHelpers = 1,
            closedAt = createdAt.plusSeconds(60),
            participations = listOf(participation(20L))
        )

        assertEquals(HelpStatus.CLOSED, request.status)
    }

    @Test
    fun `inactive participants are not counted`() {
        val request = helpRequest(
            maxHelpers = 1,
            participations = listOf(
                participation(
                    userId = 20L,
                    leftAt = createdAt.plusSeconds(60)
                )
            )
        )

        assertEquals(0, request.participantCount)
        assertEquals(HelpStatus.OPEN, request.status)
    }

    @Test
    fun `owner cannot join own help request`() {
        val request = helpRequest()

        assertFailsWith<IllegalArgumentException> {
            request.ensureCanJoin(10L)
        }
    }

    @Test
    fun `active participant cannot join twice`() {
        val request = helpRequest(
            participations = listOf(participation(20L))
        )

        assertFailsWith<IllegalArgumentException> {
            request.ensureCanJoin(20L)
        }
    }

    @Test
    fun `inactive participant can join again`() {
        val request = helpRequest(
            participations = listOf(
                participation(
                    userId = 20L,
                    leftAt = createdAt.plusSeconds(60)
                )
            )
        )

        request.ensureCanJoin(20L)
    }

    @Test
    fun `closed help request rejects new participants`() {
        val request = helpRequest(
            closedAt = createdAt.plusSeconds(60)
        )

        assertFailsWith<IllegalArgumentException> {
            request.ensureCanJoin(20L)
        }
    }

    @Test
    fun `capacity cannot be lower than active participant count`() {
        val request = helpRequest(
            maxHelpers = 2,
            participations = listOf(
                participation(20L),
                participation(30L)
            )
        )

        assertFailsWith<IllegalArgumentException> {
            request.ensureCapacityAllowed(1)
        }
    }

    @Test
    fun `capacity above twenty is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            helpRequest(maxHelpers = 21)
        }
    }

    @Test
    fun `category from another report type is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            HelpRequest(
                id = 1L,
                ownerId = 10L,
                title = "Invalid category",
                description = "This category does not belong to help requests.",
                category = ReportCategory.THEFT,
                location = Location(52.001, 4.213),
                imageUrl = null,
                viewCount = 0L,
                createdAt = createdAt,
                updatedAt = null,
                maxHelpers = 2,
                closedAt = null,
                participations = emptyList()
            )
        }
    }
}
