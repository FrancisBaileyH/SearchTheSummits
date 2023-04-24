package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ClubTreadContentExtractorStrategyTest {

    private val extractor = ClubTreadContentExtractorStrategy()

    @Test
    fun `extracts expected content from site`() {
        val html = loadHtml("clubtread/forum-post.html")
        val content = extractor.extract(html)

        Assertions.assertEquals("VI = Vancouver Island Mount Manuel Quimper Dec 1, 2022 - ClubTread Community", content.title)
        Assertions.assertTrue(content.rawText.startsWith("Mount Manuel Quimper Dec 1, 2022 Vancouver Island runts When we want to avoid the snow we head to the island."))
        Assertions.assertEquals("Vancouver Island runts When we want to avoid the snow we head to the island. Although everywhere got hit with that recent snowfall, we kept the", content.description)
        Assertions.assertEquals("", content.semanticText)
    }
}