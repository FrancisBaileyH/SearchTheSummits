package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class BCMCFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://bcmc.ca/SomeUser",
            "https://bcmc.ca/m/news/home",
            "https://bcmc.ca/m/articles/home"
        )

        val expectedNotToSkip = listOf(
            "https://bcmc.ca/m/docs/view/BCMC-Newsletter-November-2001-2012-03-17",
            "https://bcmc.ca/media/newsletters/BCMC Newsletter 2001-11.pdf",
            "https://bcmc.ca/m/docs/view/The-B-C-Mountaineer-2016",
            "https://bcmc.ca/media/newsletters/BC Mountaineer 2004.pdf",
            "https://bcmc.ca/m/docs/?t=1&search_mode=latest&sort=latest&page=1&per_page=100"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}