package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class AlpenGlowFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "http://www.alpenglow.org/ski-history/index.html",
            "http://www.alpenglow.org/",
            "http://www.alpenglow.org/test"
        )

        val expectedNotToSkip = listOf(
            "http://www.alpenglow.org/nwmj/05/issue2.html",
            "http://www.alpenglow.org/nwmj/05/051_Poltergeist.html",
            "http://www.alpenglow.org/nwmj/archive.html"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}