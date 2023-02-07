package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class NWHikersNetFilterTest: FilterTest() {

    @Test
    fun `NWHikers index filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.nwhikers.net/forums/viewtopic.php?t=8030039"
        )

        val expectedToSkip = listOf(
            "https://www.nwhikers.net/forums/viewforum.php?f=3&topicdays=0&start=50",
            "https://www.nwhikers.net/forums/viewforum.php?f=3",
            "https://www.nwhikers.net/forums/viewforum.php?f=1",
            "https://www.nwhikers.net/forums/viewforum.php?f=1&topicdays=0&start=50",
            "https://www.nwhikers.net/forums/profile.php?mode=viewprofile&u=5300"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `NWHikers filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.nwhikers.net/forums/viewforum.php?f=3&topicdays=0&start=50",
            "https://www.nwhikers.net/forums/viewforum.php?f=3",
            "https://www.nwhikers.net/forums/viewtopic.php?t=8030039"
        )

        val expectedToSkip = listOf(
            "https://www.nwhikers.net/forums/viewforum.php?f=1",
            "https://www.nwhikers.net/forums/viewforum.php?f=1&topicdays=0&start=50",
            "https://www.nwhikers.net/forums/profile.php?mode=viewprofile&u=5300"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

}