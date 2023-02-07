package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class MountEverestFoundationFilterTest: FilterTest() {

    @Test
    fun `mef org index filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4",
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4/"
        )

        val expectedToSkip = listOf(
            "https://www.mef.org.uk/expeditions/p4",
            "https://www.mef.org.uk/expeditions/p401"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `mef org filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4",
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4/",
            "https://www.mef.org.uk/expeditions/p400"
        )

        val expectedToSkip = listOf(
            "https://www.mef.org.uk",
            "https://www.mef.org.uk/about"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

}