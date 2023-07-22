package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class ScurlockFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://pbase.com/test/central_n_centralcascades"
        )

        val expectedNotToSkip = listOf(
            "https://pbase.com/nolock/central_n_centralcascades",
            "https://pbase.com/nolock/bugaboos&page=2",
            "https://pbase.com/nolock/bugaboos"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `skips indexing expected links`() {
        val expectedToSkip = listOf(
            "https://pbase.com/nolock/central_n_centralcascades",
            "https://pbase.com/nolock/bugaboos&page=2",
            "https://pbase.com/nolock/bugaboos"
        )

        val expectedNotToSkip = emptyList<String>()

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }
}