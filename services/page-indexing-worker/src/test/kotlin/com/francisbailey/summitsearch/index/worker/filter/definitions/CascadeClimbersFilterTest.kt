package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class CascadeClimbersFilterTest: FilterTest() {

    @Test
    fun `cascade climbers filter skips expected links`() {
        val expectedToSkip = listOf(
            "https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010/?do=getLastComment",
            "https://cascadeclimbers.com/forum/topic/75809-joffre-peak-flavelle-lane-8152010/",
            "https://cascadeclimbers.com/forum/profile/9797-jasong/",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/?sortby=title&sortdirection=asc&page=4"
        )

        val expectedNotToSkip = mutableListOf(
            "https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010/",
            "https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/page/2/",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/page/2",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/page/20",
            "https://cascadeclimbers.com/forum/topic/53522-tr-mt-queen-bess-se-buttress-good-queen-bes/"
        )

        val allowedTopics = listOf(
            "34-alaska",
            "16-british-columbiacanada",
            "10-north-cascades",
            "11-alpine-lakes",
            "13-southern-wa-cascades",
            "12-mount-rainier-np",
            "14-olympic-peninsula",
            "28-centraleastern-washington",
            "15-oregon-cascades",
            "41-columbia-river-gorge",
            "25-california",
            "49-idaho",
            "48-montana",
            "52-the-rest-of-the-us-and-international"
        ).map {
            "https://cascadeclimbers.com/forum/forum/$it/"
        }

        expectedNotToSkip.addAll(allowedTopics)

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}