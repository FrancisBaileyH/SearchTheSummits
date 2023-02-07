package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class DrDirtbagFilterTest: FilterTest() {

    @Test
    fun `drdirtbag skips expected links`() {
        val expectedToSkip = listOf(
            "https://www.drdirtbag.com/2015/08/04/primitive-canada/primitive-canada-2/"
        )

        val expectedNotToSkip = listOf(
            "https://www.drdirtbag.com/2015/08/04/primitive-canada/"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

}