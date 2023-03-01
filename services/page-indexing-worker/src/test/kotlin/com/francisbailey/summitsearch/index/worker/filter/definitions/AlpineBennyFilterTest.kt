package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class AlpineBennyFilterTest: FilterTest() {


    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://alpinebenny.com/guiding",
            "https://alpinebenny.com/rucksack"
        )

        val expectedNotToSkip = listOf(
            "https://alpinebenny.com/blog/2022/4/23/mooses-tooth-free-solo",
            "https://alpinebenny.com/blog"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}