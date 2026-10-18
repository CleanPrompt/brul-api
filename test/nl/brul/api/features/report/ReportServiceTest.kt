
// test/nl/brul/api/features/report/ReportServiceTest.kt
package nl.brul.api.features.report

import nl.brul.api.features.report.model.Location
import nl.brul.api.features.report.model.Report
import nl.brul.api.features.report.model.ReportCategory
import nl.brul.api.features.report.model.SafetyReport
import nl.brul.api.features.report.model.NuisanceReport
import nl.brul.api.features.report.repository.NewReport
import nl.brul.api.features.report.repository.ReportRepository
import nl.brul.api.features.report.repository.ReportWithCounts
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReportServiceTest {

    private val first = SafetyReport(
        id = 1L,
        ownerId = 10L,
        title = "Stolen bicycle",
        description = "My bicycle was stolen near the station.",
        category = ReportCategory.THEFT,
        location = Location(52.001, 4.213),
        imageUrl = null,
        viewCount = 5L,
        createdAt = Instant.parse("2026-10-18T08:00:00Z"),
        updatedAt = null
    )

    private val second = NuisanceReport(
        id = 2L,
        ownerId = 20L,
        title = "Noise disturbance",
        description = "There is loud noise every evening.",
        category = ReportCategory.NOISE,
        location = Location(52.010, 4.213),
        imageUrl = null,
        viewCount = 50L,
        createdAt = Instant.parse("2026-10-18T09:00:00Z"),
        updatedAt = null
    )

    private val third = SafetyReport(
        id = 3L,
        ownerId = 30L,
        title = "Dangerous traffic situation",
        description = "Cars drive too fast near the school.",
        category = ReportCategory.TRAFFIC_SAFETY,
        location = Location(52.100, 4.213),
        imageUrl = null,
        viewCount = 10L,
        createdAt = Instant.parse("2026-10-18T10:00:00Z"),
        updatedAt = null
    )

    private val reports = listOf(
        ReportWithCounts(
            report = first,
            likeCount = 10L,
            dislikeCount = 1L
        ),
        ReportWithCounts(
            report = second,
            likeCount = 2L,
            dislikeCount = 0L
        ),
        ReportWithCounts(
            report = third,
            likeCount = 5L,
            dislikeCount = 0L
        )
    )

    private val service = ReportService(
        FakeReportRepository(reports)
    )

    private fun search(
        q: String? = null,
        type: String? = null,
        category: String? = null,
        latitude: String? = null,
        longitude: String? = null,
        radiusKm: String? = null,
        sort: String? = null
    ): List<Long> {
        return service.findAll(
            ReportSearch(
                q = q,
                type = type,
                category = category,
                latitude = latitude,
                longitude = longitude,
                radiusKm = radiusKm,
                sort = sort
            )
        ).map { it.id }
    }

    @Test
    fun `reports are sorted newest first by default`() {
        assertEquals(
            listOf(3L, 2L, 1L),
            search()
        )
    }

    @Test
    fun `search matches report title`() {
        assertEquals(
            listOf(1L),
            search(q = "bicycle")
        )
    }

    @Test
    fun `search matches report description`() {
        assertEquals(
            listOf(3L),
            search(q = "school")
        )
    }

    @Test
    fun `search is case insensitive`() {
        assertEquals(
            listOf(2L),
            search(q = "NOISE DISTURBANCE")
        )
    }

    @Test
    fun `reports can be filtered by type`() {
        assertEquals(
            listOf(3L, 1L),
            search(type = "SAFETY")
        )
    }

    @Test
    fun `reports can be filtered by category`() {
        assertEquals(
            listOf(2L),
            search(category = "NOISE")
        )
    }

    @Test
    fun `search and filters can be combined`() {
        assertEquals(
            listOf(1L),
            search(
                q = "stolen",
                type = "SAFETY",
                category = "THEFT"
            )
        )
    }

    @Test
    fun `reports can be sorted by most likes`() {
        assertEquals(
            listOf(1L, 3L, 2L),
            search(sort = "mostLiked")
        )
    }

    @Test
    fun `reports can be sorted by most views`() {
        assertEquals(
            listOf(2L, 3L, 1L),
            search(sort = "mostViewed")
        )
    }

    @Test
    fun `reports can be sorted by nearest distance`() {
        assertEquals(
            listOf(1L, 2L, 3L),
            search(
                latitude = "52.001",
                longitude = "4.213",
                sort = "nearest"
            )
        )
    }

    @Test
    fun `radius filter excludes distant reports`() {
        assertEquals(
            listOf(2L, 1L),
            search(
                latitude = "52.001",
                longitude = "4.213",
                radiusKm = "5"
            )
        )
    }

    @Test
    fun `search returns empty list when nothing matches`() {
        assertTrue(
            search(q = "nonexistent-search-term").isEmpty()
        )
    }

    @Test
    fun `invalid sort option is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            search(sort = "random")
        }
    }

    @Test
    fun `nearest sort requires coordinates`() {
        assertFailsWith<IllegalArgumentException> {
            search(sort = "nearest")
        }
    }

    @Test
    fun `radius filter requires coordinates`() {
        assertFailsWith<IllegalArgumentException> {
            search(radiusKm = "5")
        }
    }

    @Test
    fun `radius above maximum is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            search(
                latitude = "52.001",
                longitude = "4.213",
                radiusKm = "26"
            )
        }
    }

    @Test
    fun `incompatible type and category are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            search(
                type = "SAFETY",
                category = "NOISE"
            )
        }
    }

    @Test
    fun `distance is included when coordinates are provided`() {
        val results = service.findAll(
            ReportSearch(
                q = null,
                type = null,
                category = null,
                latitude = "52.001",
                longitude = "4.213",
                radiusKm = null,
                sort = "nearest"
            )
        )

        assertEquals(0.0, results.first().distanceKm)

        assertTrue(
            results.all { it.distanceKm != null }
        )
    }

    private class FakeReportRepository(
        private val items: List<ReportWithCounts>
    ) : ReportRepository {

        override fun findAll(): List<ReportWithCounts> {
            return items
        }

        override fun create(report: NewReport): Report {
            throw UnsupportedOperationException(
                "Creating reports is not needed in this test."
            )
        }

        override fun findByIdAndIncreaseViews(
            id: Long
        ): ReportWithCounts? {
            return items.find { it.report.id == id }
        }

        override fun update(
            id: Long,
            ownerId: Long,
            change: (Report) -> NewReport
        ): ReportWithCounts? {
            throw UnsupportedOperationException(
                "Updating reports is not needed in this test."
            )
        }

        override fun delete(
            id: Long,
            userId: Long,
            isAdmin: Boolean
        ): Boolean {
            throw UnsupportedOperationException(
                "Deleting reports is not needed in this test."
            )
        }
    }
}
