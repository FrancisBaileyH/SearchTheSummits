package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class MountaineersOrgFilterTest: FilterTest() {

    @Test
    fun `skips expected links on indexing`() {
        val expectedToSkip = listOf(
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-index-to-annual-issues-1967-to-1980",
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals",
            "https://www.mountaineers.org/activities/trip-reports/@@faceted_query?b_start:int=100"
        )

        val expectedNotToSkip = listOf(
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-1969",
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-1970-1971",
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-1977-8",
            "https://www.mountaineers.org/activities/trip-reports/day-hike-cedar-butte-olallie-state-park-16"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `skips expected links on discovery`() {
        val expectedToSkip = listOf(
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-index-to-annual-issues-1967-to-1980",
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals"
        )

        val expectedNotToSkip = listOf(
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-1969",
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-1970-1971",
            "https://www.mountaineers.org/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-1977-8",
            "https://www.mountaineers.org/activities/trip-reports/@@faceted_query?b_start:int=100",
            "https://www.mountaineers.org/activities/trip-reports/day-hike-cedar-butte-olallie-state-park-16"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

}