package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class AlpineJournalUKFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "http://www.alpinejournal.org.uk/cgi-bin/search.php",
            "http://www.alpinejournal.org.uk/test/image.png"
        )

        val expectedNotToSkip = listOf(
            "http://www.alpinejournal.org.uk/Content/some-file.pdf"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}