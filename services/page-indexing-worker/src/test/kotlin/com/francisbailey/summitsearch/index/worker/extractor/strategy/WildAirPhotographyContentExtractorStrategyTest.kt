package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WildAirPhotographyContentExtractorStrategyTest {

    private val extractor = WildAirPhotographyContentExtractorStrategy()

    @Test
    fun `extracts expected content from site`() {
        val html = loadHtml("wildair/blog-post.html")
        val content = extractor.extract(html)

        assertEquals("Monashee Ski Traverse 2022", content.title)
        assertTrue(content.rawText.startsWith(" 37 days from Grand Forks to Valemount, by Douglas Noblet   May 7th 2022, Steve Senecal, Isobel Phoebus and I are walking down the Clemina Creek FSR."))
        assertEquals("", content.description)
        assertEquals("", content.semanticText)
    }

}