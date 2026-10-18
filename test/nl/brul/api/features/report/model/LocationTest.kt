
// test/nl/brul/api/features/report/model/LocationTest.kt
package nl.brul.api.features.report.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LocationTest {

    @Test
    fun `same location has zero distance`() {
        val location = Location(52.001, 4.213)

        val distance = location.distanceTo(location)

        assertEquals(0.0, distance, 0.000001)
    }

    @Test
    fun `distance between nearby locations is approximately one kilometer`() {
        val first = Location(52.001, 4.213)
        val second = Location(52.010, 4.213)

        val distance = first.distanceTo(second)

        assertTrue(distance in 0.9..1.1)
    }

    @Test
    fun `distance is equal in both directions`() {
        val first = Location(52.001, 4.213)
        val second = Location(52.020, 4.240)

        assertEquals(
            first.distanceTo(second),
            second.distanceTo(first),
            0.000001
        )
    }

    @Test
    fun `latitude outside valid range is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Location(91.0, 4.213)
        }
    }

    @Test
    fun `longitude outside valid range is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Location(52.001, 181.0)
        }
    }

    @Test
    fun `NaN coordinates are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Location(Double.NaN, 4.213)
        }
    }

    @Test
    fun `infinite coordinates are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Location(52.001, Double.POSITIVE_INFINITY)
        }
    }
}
