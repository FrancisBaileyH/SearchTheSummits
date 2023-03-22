package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class SkiSicknessFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {

        val expectedToSkip = listOf(
            "https://skisickness.com/post/memberlist.php?mode=viewprofile&u=89",
            "https://skisickness.com/post/vt615-daily-bread&view=print"
        )

        val expectedNotToSkip = listOf(
            "https://skisickness.com/post/vt572-mas-truchas",
            "https://skisickness.com/post/viewforum.php?f=3&start=50",
            "https://skisickness.com/post/viewforum.php?f=3"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `skips indexing expected links`() {
        val expectedToSkip = listOf(
            "https://skisickness.com/post/memberlist.php?mode=viewprofile&u=89",
            "https://skisickness.com/post/vt615-daily-bread&view=print",
            "https://skisickness.com/post/viewforum.php?f=3&start=50",
            "https://skisickness.com/post/viewforum.php?f=3"
        )

        val expectedNotToSkip = listOf(
            "https://skisickness.com/post/vt572-mas-truchas"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

}