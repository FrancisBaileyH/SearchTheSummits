package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class SverdinaFilterTest: FilterTest() {

    @Test
    fun `sverdina filter skips short pages and main page`() {
        val expectedToSkip = listOf(
            "http://sverdina.com/middle_sister1_SHORT.asp",
            "http://sverdina.com/climb.asp"
        )

        val expectedNotToSkip = listOf(
            "http://sverdina.com/middle_sister1.asp"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

}