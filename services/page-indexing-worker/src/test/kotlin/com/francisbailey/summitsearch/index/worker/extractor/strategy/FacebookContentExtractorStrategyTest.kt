package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FacebookContentExtractorStrategyTest {

    @Test
    fun `indexes expected content from facebook page`() {
        val expectedParagraphContent = "Mount Hansen Northwest Peak. I took a chance on the Sowerby Creek FSR hoping I could drive right on to the ridge... that was a hard no. The road deteriorates quickly and becomes atv terrain, so the road hiking began. 2.5hrs later I started making my way through the forest going over a few bumps and staying close to the center of the ridge, I eventually found a flagged route and followed that the rest of the way. After 14kms in I ended my hike at the sub Summit(Northwest peak) of Hansen and sent the drone to do the dirty work. It's probably no more then 30-40mins to the true Summit but I was done with it lol. Just over 28kms roundtrip, 1350m gain approx, 9.5hrs"
        val expectedSeoDescription = "Mount Hansen Northwest Peak. I took a chance on the Sowerby Creek FSR hoping I could drive right on to the ridge... that was a hard no. The road deteriorates quickly and becomes atv terrain, so the..."
        val document = loadHtml("facebook/facebook-group-post.html")

        val strategy = FacebookContentExtractorStrategy()

        val documentContent = strategy.extract(document)

        assertEquals(documentContent.semanticText, expectedParagraphContent)
        assertEquals(documentContent.description, expectedSeoDescription)
        assertEquals(documentContent.title, "SWBC Peak Baggers | Mount Hansen Northwest Peak")
    }
}