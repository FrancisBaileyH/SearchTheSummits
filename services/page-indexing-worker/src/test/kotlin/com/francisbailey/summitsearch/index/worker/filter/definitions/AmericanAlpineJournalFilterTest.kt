package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class AmericanAlpineJournalFilterTest: FilterTest() {

    @Test
    fun `american alpine journal skips expected links on indexing`() {
        val expectedToSkip = listOf(
            "https://publications.americanalpineclub.org/test",
            "https://publications.americanalpineclub.org/articles?page=4"
        )

        val expectedNotToSkip = listOf(
            "https://publications.americanalpineclub.org/articles/12196347903",
            "https://publications.americanalpineclub.org/articles/12196347903/"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `american alpine journal skips expected links on discovery`() {
        val expectedToSkip = listOf(
            "https://publications.americanalpineclub.org/test"
        )

        val expectedNotToSkip = listOf(
            "https://publications.americanalpineclub.org/articles/12196347903",
            "https://publications.americanalpineclub.org/articles/12196347903/",
            "https://publications.americanalpineclub.org/articles?page=40"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

}