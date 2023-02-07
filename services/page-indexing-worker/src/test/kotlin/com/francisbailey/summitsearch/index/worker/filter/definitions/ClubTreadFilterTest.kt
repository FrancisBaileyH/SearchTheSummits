package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class ClubTreadFilterTest: FilterTest() {

    @Test
    fun `club tread filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "27-british-columbia",
            "130-canadian-rockies",
            "34-alberta",
            "37-washington-state",
            "35-other-regions"
        ).map {
            "https://forums.clubtread.com/$it/"
        }.toMutableList()

        expectedNotToSkip.add("https://forums.clubtread.com/27-british-columbia/95172-watersprite-lake-2022-09-25-a.html")
        expectedNotToSkip.add("https://forums.clubtread.com/27-british-columbia/40728-ain-t-elfin-lakes-cayoosh-creek-hut.html")
        expectedNotToSkip.add("https://forums.clubtread.com/130-canadian-rockies/index2.html") // paginated topic
        expectedNotToSkip.add("https://forums.clubtread.com/130-canadian-rockies/index20.html") // paginated topic
        expectedNotToSkip.add("https://forums.clubtread.com/27-british-columbia/43281-t-tower-t-tiara.html")

        val expectedToSkip = listOf(
            "https://forums.clubtread.com/27-british-columbia/40728-ain-t-elfin-lakes-cayoosh-creek-hut-2.html",
            "https://forums.clubtread.com/newreply.php?do=newreply&p=476682",
            "https://forums.clubtread.com/members/71772-losthiker.html",
            "https://forums.clubtread.com/27-british-columbia/95172-watersprite-lake-2022-09-25-a-print.html",
            "https://forums.clubtread.com/27-british-columbia/44397-corral-trail-memorial-lookout-mar-03-13-a-prev-thread.html",
            "https://forums.clubtread.com/27-british-columbia/44397-corral-trail-memorial-lookout-mar-03-13-a-next-thread.html"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

}