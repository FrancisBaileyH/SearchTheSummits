package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class StephAbeggFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://sites.google.com/stephabegg.com/britishcolumbia/tripreports",
            "https://sites.google.com/stephabegg.com/britishcolumbia/projects",
            "https://sites.google.com/example.com/britishcolumbia/projects",
            "https://sites.google.com/stephabegg.com/chronology/tripreports/chronology"
        )

        val expectedNotToSkip = listOf(
            "https://sites.google.com/stephabegg.com/washington/tripreports/aasgardsentinel1",
            "https://sites.google.com/stephabegg.com/colorado/tripreports/combat"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `skips expected discovery links`() {
        val expectedToSkip = listOf(
            "https://sites.google.com/example.com/britishcolumbia/projects"
        )

        val expectedNotToSkip = listOf(
            "https://sites.google.com/stephabegg.com/britishcolumbia/tripreports",
            "https://sites.google.com/stephabegg.com/britishcolumbia/projects",
            "https://sites.google.com/stephabegg.com/washington/tripreports/aasgardsentinel1",
            "https://sites.google.com/stephabegg.com/colorado/tripreports/combat"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}