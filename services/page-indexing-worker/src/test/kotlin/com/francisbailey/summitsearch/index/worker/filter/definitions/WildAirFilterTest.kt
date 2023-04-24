package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class WildAirFilterTest: FilterTest() {

    @Test
    fun `filters expected links`() {
        val expectedNotToSkip = listOf(
            "https://wildairphoto.com/stories",
            "https://wildairphoto.com/stories/f/under-the-badshots"
        )

        val expectedToSkip = listOf(
            "https://wildairphoto.com/photo-galleries",
            "https://wildairphoto.com",
            "https://wildairphoto.com/services"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}