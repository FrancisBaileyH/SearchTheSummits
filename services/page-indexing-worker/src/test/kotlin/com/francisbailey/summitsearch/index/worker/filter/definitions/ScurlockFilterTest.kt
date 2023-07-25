package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class ScurlockFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://pbase.com/test/central_n_centralcascades",
            "https://pbase.com/nolock/image/145217821/large",
            "https://pbase.com/nolock/image/145217821/small",
            "https://pbase.com/nolock/image/145217821/other"
        )

        val expectedNotToSkip = listOf(
            "https://pbase.com/nolock/central_n_centralcascades",
            "https://pbase.com/nolock/bugaboos",
            "https://pbase.com/nolock/image/145217821"
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