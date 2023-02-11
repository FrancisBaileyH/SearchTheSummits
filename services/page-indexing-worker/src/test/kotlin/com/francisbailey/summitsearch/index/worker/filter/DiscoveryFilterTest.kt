package com.francisbailey.summitsearch.index.worker.filter

import com.francisbailey.summitsearch.index.worker.extension.normalizeAndEncode
import com.francisbailey.summitsearch.index.worker.filter.definitions.DefaultFilterChain
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import java.net.URL
import java.util.regex.Pattern

class CrawlerFilterTest {

    @Test
    fun `runs inclusive filter chain associated with host`() {
        val filterService = DocumentFilterService(DefaultFilterChain)
        val path = "/include/this/path/only"
        val host = "www.francisbaileyh.com"

        val inclusiveChain = DocumentFilterChain(exclusive = false).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^$path$")))
        }

        val url = URL("https://$host")
        filterService.addFilterChain(url, inclusiveChain)

        assertFalse(filterService.shouldFilter(URL("https://$host$path")))
        assertTrue(filterService.shouldFilter(URL("https://$host/include")))
    }

    @Test
    fun `runs exclusive filter chain associated with host`() {
        val filterService = DocumentFilterService(DefaultFilterChain)
        val path = "/include/this/path/only"
        val host = "www.francisbaileyh.com"

        val inclusiveChain = DocumentFilterChain(exclusive = true).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^$path$")))
        }

        val url = URL("https://$host")
        filterService.addFilterChain(url, inclusiveChain)

        assertTrue(filterService.shouldFilter(URL("https://$host$path")))
        assertFalse(filterService.shouldFilter(URL("https://$host/include")))
    }

    @Test
    fun `does not run default chain if chain exists for host`() {
        val defaultChain = mock<DocumentFilterChain>()
        val filterService = DocumentFilterService(defaultChain)

        val inclusiveChain = DocumentFilterChain(exclusive = false).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile(".*")))
        }

        filterService.addFilterChain(URL("https://francisbaileyh.com"), inclusiveChain)
        filterService.shouldFilter(URL("https://francisbaileyh.com/test"))

        verifyNoInteractions(defaultChain)
    }

    @Test
    fun `merge adds rules to chain`() {
        val url = URL("https://francisbailey.com/test")
        val chain = DocumentFilterChain(exclusive = true)

        assertFalse(chain.shouldFilter(url))

        val chainToMerge = DocumentFilterChain(exclusive = true)
        chainToMerge.addFilter(PathMatchingDocumentFilter(Pattern.compile("^/test$")))

        chain.merge(chainToMerge)

        assertTrue(chain.shouldFilter(url))
    }

    @Test
    fun `safely handles white spaces in URLs`() {
        val url = URL("https://francisbailey.com/test/test with spaces here.pdf").normalizeAndEncode()

        val filterService = DocumentFilterService(DefaultFilterChain)

        val inclusiveChain = DocumentFilterChain(exclusive = true).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile(".*")))
        }

        filterService.addFilterChain(url, inclusiveChain)

        assertTrue(filterService.shouldFilter(url))
    }

    @Test
    fun `safely handles already encoded URLs`() {
        val url = URL("https://francisbailey.com/test/test%20with%20spaces%20here.pdf").normalizeAndEncode()

        val filterService = DocumentFilterService(DefaultFilterChain)

        val inclusiveChain = DocumentFilterChain(exclusive = true).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile(".*")))
        }

        filterService.addFilterChain(url, inclusiveChain)

        assertTrue(filterService.shouldFilter(url))
    }

}