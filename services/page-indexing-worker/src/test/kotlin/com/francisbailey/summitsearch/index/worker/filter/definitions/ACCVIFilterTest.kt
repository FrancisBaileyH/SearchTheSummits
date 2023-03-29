package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class ACCVIFilterTest: FilterTest() {

    @Test
    fun `skips expected links for ACCVI`() {
        val expectedToSkip = listOf(
            "https://accvi.ca/",
            "https://discourse.accvi.ca/u/1234",
            "https://accvi.ca/programs/new-members/"
        )

        val expectedNotToSkip = listOf(
            "https://discourse.accvi.ca/c/trip-report/5",
            "https://discourse.accvi.ca/t/mount-allan-brooks-snowshoe/622",
            "https://discourse.accvi.ca/c/trip-report/5?page=2",
            "https://accvi.ca/wp-content/bushwhacker/accvi_iba_1992.pdf",
            "https://accvi.ca/wp-content/uploads/ACCVI-IBA-2021a.pdf"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `skips indexing expected links for ACCVI Discourse`() {
        val expectedToSkip = listOf(
            "https://discourse.accvi.ca/c/trip-report/5",
            "https://discourse.accvi.ca/c/trip-report/5?page=2"
        )

        val expectedNotToSkip = listOf(
            "https://discourse.accvi.ca/t/mount-allan-brooks-snowshoe/622"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }
}