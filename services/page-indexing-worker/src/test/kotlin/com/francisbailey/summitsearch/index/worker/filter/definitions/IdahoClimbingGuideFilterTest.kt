package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class IdahoClimbingGuideFilterTest: FilterTest() {

    @Test
    fun `idaho climbing guides index filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.idahoaclimbingguide.com/bookupdates/peak-7623-by-mike-hays/",
            "https://www.idahoaclimbingguide.com/bookupdates/peak-7623-by-mike-hays"
        )

        val expectedToSkip = listOf(
            "https://www.idahoaclimbingguide.com/wp-content/uploads/PICT000144-350x370.jpg"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `idaho climbing guides filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.idahoaclimbingguide.com/peak-lists/",
            "https://www.idahoaclimbingguide.com/peak-index/?fcoby=elevd&alpha=0&ecomp=6000"
        )

        val expectedToSkip = listOf(
            "https://www.idahoaclimbingguide.com/wp-content/uploads/PICT000144-350x370.jpg"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}