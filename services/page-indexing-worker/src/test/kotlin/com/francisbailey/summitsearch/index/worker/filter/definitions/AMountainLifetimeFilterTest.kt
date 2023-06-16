package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class AMountainLifetimeFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://amountainlifetime.net/img/gallery/41/2022/244/P9030761.jpg",
            "https://amountainlifetime.net/content/modals/stories.php?SID=244"
        )

        val expectedNotToSkip = listOf(
            "https://amountainlifetime.net/content/gallery/244/peak-lake-and-bow-mountain-2022"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }
}