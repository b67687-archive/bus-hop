package com.bushop.sg.data.local

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Regression tests for [BusStopIndex.search] with realistic Singapore bus stop data. */
class BusStopIndexTest {
    private lateinit var index: BusStopIndex

    /** Realistic Singapore bus stops with varied naming patterns. */
    private val sampleStops =
        listOf(
            BusStopEntry("12341", "Jurong East Int", "Jurong Gateway Rd"),
            BusStopEntry("12342", "Opp Blk 123 Jurong", "Jurong Ave 1"),
            BusStopEntry("22345", "Bt Batok Int", "Bt Batok Ave"),
            BusStopEntry("22346", "Bt Batok Stn", "Bt Batok Ave"),
            BusStopEntry("33456", "Clementi Int", "Clementi Ave 3"),
            BusStopEntry("33457", "Clementi Stn Exit A", "Clementi Ave 3"),
            BusStopEntry("55678", "Tampines Int", "Tampines Ave 4"),
            BusStopEntry("55679", "Tampines Stn Exit B", "Tampines Ave 4"),
            BusStopEntry("66789", "Opp Blk 456", "Woodlands Ave 5"),
            BusStopEntry("66790", "Blk 456", "Woodlands Ave 5"),
            BusStopEntry("88901", "St. Michael's Sch", "St. Michael's Rd"),
            BusStopEntry("99012", "Blk 789", "Ang Mo Kio Ave 3"),
            BusStopEntry("99013", "Ang Mo Kio Stn", "Ang Mo Kio Ave 3"),
            BusStopEntry("99014", "Ang Mo Kio Int", "Ang Mo Kio Ave 8"),
            BusStopEntry("11111", "Orchard Stn", "Orchard Blvd"),
            BusStopEntry("11112", "Orchard Stn Exit B", "Orchard Blvd"),
            BusStopEntry("22222", "HarbourFront Int", "Telok Blangah Rd"),
            BusStopEntry("33333", "Choa Chu Kang Int", "Choa Chu Kang Loop"),
            BusStopEntry("33334", "Choa Chu Kang Stn", "Choa Chu Kang Ave 4"),
            BusStopEntry("44444", "Boon Lay Int", "Boon Lay Way"),
            BusStopEntry("44445", "Boon Lay Stn", "Boon Lay Way"),
            BusStopEntry("55555", "Punggol Temp Int", "Punggol Ctrl"),
            BusStopEntry("55556", "Punggol Stn", "Punggol Ctrl"),
            BusStopEntry("66666", "Sengkang Int", "Sengkang Square"),
            BusStopEntry("66667", "Sengkang Stn", "Sengkang Square"),
        )

    @Before
    fun setUp() {
        index = BusStopIndex(mockk(relaxed = true))
        index.setTestData(sampleStops)
    }

    // ── Basic matching ──

    @Test
    fun `exact name match returns top result`() {
        val results = index.search("Jurong East Int")
        assertTrue(results.isNotEmpty())
        assertEquals("12341", results.first().code)
    }

    @Test
    fun `name prefix match works`() {
        val results = index.search("Jurong")
        assertTrue(results.any { it.code == "12341" })
        assertTrue(results.any { it.code == "12342" })
    }

    @Test
    fun `multi word tokenized matching works`() {
        val results = index.search("blk jurong")
        assertEquals("12342", results.first().code)
    }

    @Test
    fun `substring match works`() {
        val results = index.search("Tampines")
        assertEquals("55678", results.first().code)
    }

    @Test
    fun `digit query finds block numbers in names`() {
        val results = index.search("789")
        assertTrue(results.any { it.code == "99012" })
    }

    @Test
    fun `digit query finds number in road names`() {
        val results = index.search("123")
        assertTrue(results.any { it.code == "12342" })
    }

    // ── Singapore abbreviation expansion ──

    @Test
    fun `abbreviation bt matches bukit`() {
        val results = index.search("bt batok")
        assertTrue(
            "'bt batok' should match Bt Batok stops",
            results.any { it.code == "22345" },
        )
    }

    @Test
    fun `abbreviation int matches interchange`() {
        val results = index.search("jurong interchange")
        assertTrue(
            "'jurong interchange' should find Jurong East Int",
            results.any { it.code == "12341" },
        )
    }

    @Test
    fun `abbreviation blk matches block`() {
        val results = index.search("block 456")
        assertTrue(
            "'block 456' should find Blk 456 stops",
            results.any { it.code == "66790" },
        )
    }

    @Test
    fun `abbreviation ave matches avenue in road`() {
        // Ave 3 should match Clementi Ave 3 stops
        val results = index.search("clementi avenue 3")
        assertTrue(
            "'clementi avenue 3' should find Clementi Ave 3 stops",
            results.any { it.code == "33456" },
        )
    }

    @Test
    fun `abbreviation rd matches road`() {
        val results = index.search("woodlands road")
        assertTrue(
            "'woodlands road' should find Woodlands Ave 5 stops",
            results.any { it.code == "66789" },
        )
    }

    @Test
    fun `abbreviation amk matches ang mo kio`() {
        val results = index.search("amk")
        assertTrue(
            "'amk' should find Ang Mo Kio stops",
            results.any { it.code == "99012" },
        )
    }

    @Test
    fun `abbreviation cck matches choa chu kang`() {
        val results = index.search("cck")
        assertTrue(
            "'cck' should find Choa Chu Kang stops",
            results.any { it.code == "33333" },
        )
    }

    // ── Typo tolerance ──

    @Test
    fun `typo in name returns match via levenshtein`() {
        val results = index.search("Jurog")
        assertTrue("Typo 'Jurog' should find Jurong", results.any { it.code == "12341" })
    }

    @Test
    fun `short query with typo works`() {
        val results = index.search("Batk")
        assertTrue(results.any { it.code == "22345" })
    }

    @Test
    fun `two edit typo still matches long words`() {
        val results = index.search("HabourFront")
        assertTrue(
            "'HabourFront' should find HarbourFront via 2-edit",
            results.any { it.code == "22222" },
        )
    }

    @Test
    fun `typo in abbreviated name`() {
        val results = index.search("bt batak")
        assertTrue(
            "'bt batak' should find Bt Batok via typo",
            results.any { it.code == "22345" },
        )
    }

    @Test
    fun `missing letter typo`() {
        val results = index.search("Tampnes")
        assertTrue(
            "'Tampnes' should find Tampines stops",
            results.any { it.code == "55678" },
        )
    }

    @Test
    fun `extra letter typo`() {
        val results = index.search("Clementii")
        assertTrue(
            "'Clementii' should find Clementi stops",
            results.any { it.code == "33456" },
        )
    }

    // ── Ranking and ordering ──

    @Test
    fun `name match beats road match`() {
        val results = index.search("Jurong")
        val jurongEastIdx = results.indexOfFirst { it.code == "12341" }
        val blkJurongIdx = results.indexOfFirst { it.code == "12342" }
        assertTrue("Name match before road match", jurongEastIdx < blkJurongIdx)
    }

    @Test
    fun `exact match ranks highest`() {
        val results = index.search("Bt Batok Int")
        assertEquals("Exact match should be first", "22345", results.first().code)
    }

    @Test
    fun `all tokens matched bonus boosts multi-token query`() {
        val results = index.search("ang mo kio")
        // AMK-named stops (with name bonus) should rank in top 5
        val topCodes = results.take(5).map { it.code }
        assertTrue(
            "Ang Mo Kio-named stops should be in top 5",
            topCodes.any { it == "99013" || it == "99014" },
        )
        // Blk 789 at Ang Mo Kio Ave 3 should appear somewhere (road match)
        assertTrue(
            "Blk 789 (Ang Mo Kio Ave 3) should appear in results",
            results.any { it.code == "99012" },
        )
    }

    @Test
    fun `station abbreviation ranks stops near stations`() {
        val results = index.search("clementi stn")
        assertTrue(
            "should find Clementi Stn Exit A",
            results.any { it.code == "33457" },
        )
    }

    // ── Edge cases ──

    @Test
    fun `code prefix match works as fallback`() {
        val results = index.search("223")
        assertTrue(results.any { it.code == "22345" })
    }

    @Test
    fun `punctuation tokenised correctly`() {
        val results = index.search("St Michael")
        assertTrue(results.any { it.code == "88901" })
    }

    @Test
    fun `apostrophe in name handled`() {
        val results = index.search("St Michaels Sch")
        assertTrue(
            "Should find St. Michael's Sch via partial tokens",
            results.any { it.code == "88901" },
        )
    }

    @Test
    fun `empty query returns empty`() {
        assertTrue(index.search("").isEmpty())
    }

    @Test
    fun `no matching query returns empty`() {
        assertTrue(index.search("ZZZZNOTHING").isEmpty())
    }

    @Test
    fun `case insensitive matching`() {
        assertEquals("12341", index.search("JURONG EAST INT").firstOrNull()?.code)
    }

    @Test
    fun `mixed case matching`() {
        assertEquals("12341", index.search("jUrOnG eAsT iNt").firstOrNull()?.code)
    }

    @Test
    fun `max 20 results returned`() {
        index.setTestData(
            (1..30).map { i ->
                BusStopEntry("${10000 + i}", "Jurong Stop $i", "Jurong Rd")
            },
        )
        assertEquals(20, index.search("Jurong").size)
    }

    @Test
    fun `single char query returns results`() {
        assertTrue(index.search("J").isNotEmpty())
    }

    @Test
    fun `hyphenated query`() {
        val results = index.search("St.-Michael")
        assertTrue("hyphen should be tokenised", results.any { it.code == "88901" })
    }

    @Test
    fun `partial road name`() {
        val results = index.search("blangah")
        assertTrue(
            "'blangah' should find Telok Blangah Rd",
            results.any { it.code == "22222" },
        )
    }

    // ── Realistic user scenarios ──

    @Test
    fun `scenario user types jurong east`() {
        val results = index.search("jurong east")
        assertEquals(
            "Jurong East Int should be top result",
            "12341",
            results.first().code,
        )
    }

    @Test
    fun `scenario user types boon lay`() {
        val results = index.search("boon lay")
        assertTrue(
            "should find Boon Lay stops",
            results.any { it.code == "44444" },
        )
    }

    @Test
    fun `scenario user types punggol`() {
        val results = index.search("punggol")
        assertTrue(
            "should find Punggol stops",
            results.any { it.code == "55555" },
        )
    }

    @Test
    fun `scenario user types sengkang int`() {
        val results = index.search("sengkang int")
        assertEquals(
            "Sengkang Int should be top",
            "66666",
            results.first().code,
        )
    }

    @Test
    fun `scenario user types choa chu kang`() {
        val results = index.search("choa chu kang")
        assertTrue(
            "should find CCK stops",
            results.any { it.code == "33333" },
        )
    }

    @Test
    fun `scenario user types orchard`() {
        val results = index.search("orchard")
        assertTrue(
            "should find Orchard stops",
            results.any { it.code == "11111" },
        )
    }

    // ── Speed / performance ──

    @Test
    fun `search completes under 5ms with 25 stops`() {
        val warmup = index.search("jurong")
        assertNotNull(warmup)

        val start = System.nanoTime()
        val iterations = 1000
        repeat(iterations) {
            index.search("jurong")
            index.search("tampines")
            index.search("clementi stn")
            index.search("bt batok")
            index.search("ang mo kio")
            index.search("choa chu kang")
            index.search("orchard stn")
            index.search("punggol temp")
            index.search("boon lay int")
            index.search("habourfront")
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        val perQuery = elapsed / (iterations * 10)
        assertTrue("Per-query speed $perQuery ms should be under 0.2 ms", perQuery < 0.2)
    }

    @Test
    fun `search under 2ms with 1000 synthetic stops`() {
        val largeSet =
            (1..1000).map { i ->
                val area =
                    listOf(
                        "Jurong",
                        "Tampines",
                        "Clementi",
                        "Woodlands",
                        "Ang Mo Kio",
                        "Bedok",
                        "Pasir Ris",
                        "Hougang",
                        "Sengkang",
                        "Punggol",
                    )[i % 10]
                BusStopEntry("${10000 + i}", "$area Blk $i", "$area Ave ${i % 50}")
            }
        index.setTestData(largeSet)

        // Warmup
        repeat(100) {
            index.search("jurong")
            index.search("woodlands")
        }

        val queries =
            listOf(
                "jurong",
                "woodlands",
                "tampines",
                "clementi",
                "ang mo kio",
                "blk 500",
                "bedok",
                "pasir ris",
                "hougang",
                "sengkang punggol",
                "jurong blk",
                "clementi ave",
                "tampines blk",
                "woodlands ave",
                "ang mo kio blk",
            )

        val start = System.nanoTime()
        val iterations = 500
        repeat(iterations) {
            for (q in queries) index.search(q)
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        val perQuery = elapsed / (iterations * queries.size)
        println("1000-stop benchmark: ${"%.3f".format(perQuery)} ms/query total ${"%.0f".format(elapsed)} ms")
        assertTrue("1000-stop per-query $perQuery ms should be under 2 ms", perQuery < 2.0)
    }

    // ── Nearby stops ──

    @Test
    fun `findNearby returns stops within radius`() {
        index.setTestData(
            listOf(
                BusStopEntry("12341", "Jurong East Int", "Jurong Gateway Rd", lat = 1.3333, lng = 103.7428),
                BusStopEntry("12342", "Opp Blk 123", "Jurong Ave 1", lat = 1.3350, lng = 103.7450),
                BusStopEntry("99001", "Far Stop", "Far Rd", lat = 1.4000, lng = 103.8000),
            ),
        )
        val nearby = index.findNearby(1.3335, 103.7430, radiusKm = 0.5)
        assertTrue(nearby.any { it.code == "12341" })
        assertTrue(nearby.any { it.code == "12342" })
        assertFalse(nearby.any { it.code == "99001" })
    }

    @Test
    fun `findNearby returns empty when no stops nearby`() {
        index.setTestData(
            listOf(
                BusStopEntry("99001", "Far Stop", "Far Rd", lat = 10.0, lng = 120.0),
            ),
        )
        assertTrue(index.findNearby(1.3, 103.7, radiusKm = 0.5).isEmpty())
    }

    @Test
    fun `findNearby returns multiple stops sorted by distance`() {
        index.setTestData(
            listOf(
                BusStopEntry("A", "Far", "Far Rd", lat = 1.4500, lng = 103.8600),
                BusStopEntry("B", "Mid", "Mid Rd", lat = 1.3800, lng = 103.7900),
                BusStopEntry("C", "Close", "Close Rd", lat = 1.3400, lng = 103.7500),
            ),
        )
        val nearby = index.findNearby(1.3335, 103.7430, radiusKm = 20.0)
        assertTrue(nearby.size >= 3)
        assertEquals("C", nearby[0].code)
        assertEquals("B", nearby[1].code)
        assertEquals("A", nearby[2].code)
    }
}
