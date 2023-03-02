package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class Explor8ionFilterTest: FilterTest() {


    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://www.explor8ion.com/bp-ski-tour-2020-002"
        )

        val expectedNotToSkip = listOf(
            "https://www.explor8ion.com/2023/02/18/poltergeist-peak/",
            "https://www.explor8ion.com/2023/02/18/poltergeist-peak"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

}