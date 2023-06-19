package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class WildIsleFilterTest: FilterTest() {

    @Test
    fun `filters expected links`() {
        val expectedNotToSkip = listOf(
            "https://wildisle.ca/blog/?page_id=519",
            "https://wildisle.ca/blog/?page_id=59"
        )

        val expectedToSkip = listOf(
            "https://wildisle.ca/",
            "https://wildisle.ca/photography/index.html",
            "https://wildisle.ca/strathcona-journals/index.html"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}