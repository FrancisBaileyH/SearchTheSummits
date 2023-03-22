package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class SuperTopoFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "http://www.supertopo.com/inc/view_profile.php?dcid=MDo-PTw_PSk,",
            "http://www.supertopo.com/rock-climbing/pro_photos.php"
        )

        val expectedNotToSkip = listOf(
            "http://www.supertopo.com/climbing/forum_trip_reports.php?s=views&o=DESC&v=1&cur=20&ftr=#list",
            "http://www.supertopo.com/tr/West-Face-of-Starr-King-Hidden-Gem-of-Yosemite/t10616n.html"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `skips indexing expected links`() {
        val expectedToSkip = listOf(
            "http://www.supertopo.com/inc/view_profile.php?dcid=MDo-PTw_PSk,",
            "http://www.supertopo.com/rock-climbing/pro_photos.php",
            "http://www.supertopo.com/climbing/forum_trip_reports.php?s=views&o=DESC&v=1&cur=20&ftr=#list"
            )

        val expectedNotToSkip = listOf(
            "http://www.supertopo.com/tr/West-Face-of-Starr-King-Hidden-Gem-of-Yosemite/t10616n.html"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }
}