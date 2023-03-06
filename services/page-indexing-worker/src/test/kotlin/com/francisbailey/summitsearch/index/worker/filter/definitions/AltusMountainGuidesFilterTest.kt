package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class AltusMountainGuidesFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://altusmountainguides.com",
            "https://altusmountainguides.com/general/",
            "https://altusmountainguides.com/general"
        )

        val expectedNotToSkip = emptyList<String>()

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}