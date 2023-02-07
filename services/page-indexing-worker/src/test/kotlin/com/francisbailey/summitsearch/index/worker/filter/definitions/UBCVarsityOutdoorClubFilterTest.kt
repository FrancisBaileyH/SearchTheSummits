package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class UBCVarsityOutdoorClubFilterTest: FilterTest() {

    @Test
    fun `voc filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.ubc-voc.com/2022/07/23/duffey-double-crown",
            "https://www.ubc-voc.com/2022/11/02/climbing-skywalker-5-8-in-memory-of-will-ungar"
        )


        val expectedToSkip = listOf(
            "https://www.ubc-voc.com/tag/first-snow",
            "https://www.ubc-voc.com/phorum5/index.php",
            "https://www.ubc-voc.com/gallery/main.php?g2_itemId=613635",
            "https://www.ubc-voc.com/category/trip-reports/cycle,paddle",
            "https://www.ubc-voc.com/wiki/Main_Page"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

}