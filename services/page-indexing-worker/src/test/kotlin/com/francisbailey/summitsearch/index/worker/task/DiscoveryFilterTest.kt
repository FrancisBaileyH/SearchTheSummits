package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.configuration.CascadeClimbersFilter
import com.francisbailey.summitsearch.index.worker.configuration.ClubTreadFilter
import com.francisbailey.summitsearch.index.worker.configuration.DefaultFilterChain
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import java.net.URL
import java.util.regex.Pattern

class CrawlerFilterTest {

    @Test
    fun `default filter skips wordpress uploads and page images`() {
        val urlString = "https://francisbaileyh.com"

        val pathsToExclude = listOf(
            "/wp-content/test",
            "/author/francis",
            "/tag/some-tag",
            "/category/some-category",
            "/some-file.jpg",
            "/some-file.gif",
            "/some-file.jpeg",
            "/some-file.png",
            "/some-file.jPeG",
            "/some-file?query=x"
        )

        pathsToExclude.forEach {
            val url = URL("$urlString$it")
            assertTrue(DefaultFilterChain.shouldFilter(url))
        }

        val url = URL("$urlString/some-normal-page")
        assertFalse(DefaultFilterChain.shouldFilter(url))
    }

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
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/page/2"
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

        expectedToSkip.forEach {
            assertTrue(CascadeClimbersFilter.shouldFilter(URL(it)))
        }

        expectedNotToSkip.forEach {
            assertFalse(CascadeClimbersFilter.shouldFilter(URL(it)))
        }
    }

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

        val expectedToSkip = listOf(
            "https://forums.clubtread.com/27-british-columbia/40728-ain-t-elfin-lakes-cayoosh-creek-hut-2.html",
            "https://forums.clubtread.com/newreply.php?do=newreply&p=476682",
            "https://forums.clubtread.com/members/71772-losthiker.html",
            "https://forums.clubtread.com/27-british-columbia/95172-watersprite-lake-2022-09-25-a-print.html"
        )

        expectedToSkip.forEach {
            assertTrue(ClubTreadFilter.shouldFilter(URL(it)))
        }

        expectedNotToSkip.forEach {
            println(it)
            assertFalse(ClubTreadFilter.shouldFilter(URL(it)))
        }
    }

    @Test
    fun `runs inclusive filter chain associated with host`() {
        val filterService = LinkDiscoveryFilterService(DefaultFilterChain)
        val path = "/include/this/path/only"
        val host = "www.francisbaileyh.com"

        val inclusiveChain = LinkDiscoveryFilterChain(exclusive = false).apply {
            addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^$path$")))
        }

        val url = URL("https://$host")
        filterService.addFilterChain(url, inclusiveChain)

        assertFalse(filterService.shouldFilter(URL("https://$host$path")))
        assertTrue(filterService.shouldFilter(URL("https://$host/include")))
    }

    @Test
    fun `runs exclusive filter chain associated with host`() {
        val filterService = LinkDiscoveryFilterService(DefaultFilterChain)
        val path = "/include/this/path/only"
        val host = "www.francisbaileyh.com"

        val inclusiveChain = LinkDiscoveryFilterChain(exclusive = true).apply {
            addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^$path$")))
        }

        val url = URL("https://$host")
        filterService.addFilterChain(url, inclusiveChain)

        assertTrue(filterService.shouldFilter(URL("https://$host$path")))
        assertFalse(filterService.shouldFilter(URL("https://$host/include")))
    }

    @Test
    fun `does not run default chain if chain exists for host`() {
        val defaultChain = mock<LinkDiscoveryFilterChain>()
        val filterService = LinkDiscoveryFilterService(defaultChain)

        val inclusiveChain = LinkDiscoveryFilterChain(exclusive = false).apply {
            addFilter(PathMatchingDiscoveryFilter(Pattern.compile(".*")))
        }

        filterService.addFilterChain(URL("https://francisbaileyh.com"), inclusiveChain)
        filterService.shouldFilter(URL("https://francisbaileyh.com/test"))

        verifyNoInteractions(defaultChain)
    }

}