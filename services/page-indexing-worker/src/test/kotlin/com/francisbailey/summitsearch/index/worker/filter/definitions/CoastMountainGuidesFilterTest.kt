package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class CoastMountainGuidesFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://coastmountainguides.com/blog/",
            "https://coastmountainguides.com/",
            "https://coastmountainguides.com/some-page"
        )

        val expectedNotToSkip = emptyList<String>()

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}