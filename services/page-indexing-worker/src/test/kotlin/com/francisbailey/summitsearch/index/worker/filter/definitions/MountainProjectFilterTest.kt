package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class MountainProjectFilterTest: FilterTest() {

    @Test
    fun `filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.mountainproject.com/route/107063080/frader-pisafe-aka-salvaterra",
            "https://www.mountainproject.com/area/112947492/santa-cruz"
        )

        val expectedToSkip = listOf(
            "https://www.mountainproject.com/user/106731034/phil-esra",
            "https://www.mountainproject.com/route/stats/107063080/frader-pisafe-aka-salvaterra",
            "https://www.mountainproject.com/photo/111593979/ina-my-village-we-call-this-splitorsky",
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `filter skips expected links on index`() {
        val expectedNotToSkip = listOf(
            "https://www.mountainproject.com/route/107063080/frader-pisafe-aka-salvaterra"
        )

        val expectedToSkip = listOf(
            "https://www.mountainproject.com/user/106731034/phil-esra",
            "https://www.mountainproject.com/route/stats/107063080/frader-pisafe-aka-salvaterra",
            "https://www.mountainproject.com/photo/111593979/ina-my-village-we-call-this-splitorsky",
            "https://www.mountainproject.com/area/112947492/santa-cruz"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }
}