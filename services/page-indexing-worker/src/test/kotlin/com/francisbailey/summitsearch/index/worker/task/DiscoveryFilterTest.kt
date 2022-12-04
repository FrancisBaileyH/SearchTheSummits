package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.crawler.DefaultFilterChain
import com.francisbailey.summitsearch.index.worker.crawler.LinkDiscoveryFilterChain
import com.francisbailey.summitsearch.index.worker.crawler.LinkDiscoveryFilterService
import com.francisbailey.summitsearch.index.worker.crawler.PathMatchingDiscoveryFilter
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
            "/some-file.jPeG"
        )

        pathsToExclude.forEach {
            val url = URL("$urlString$it")
            assertTrue(DefaultFilterChain.shouldFilter(url))
        }

        val url = URL("$urlString/some-normal-page")
        assertFalse(DefaultFilterChain.shouldFilter(url))
    }

    @Test
    fun `runs inclusive filter chain associated with host`() {
        val filterService = LinkDiscoveryFilterService()
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
        val filterService = LinkDiscoveryFilterService()
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