package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AMountainLifetimeImageExtractorStrategyTest {

    private val extractor = AMountainLifeTimeImageExtractorStrategy()

    @Test
    fun `extracts expected images`() {
        val document = loadHtml("amountainlifetime/page.html")

        val expectedImages = listOf(
            CaptionedImage(
                caption = "EARL ALDERSON SKINNING INTO GARNET CANYON FOR AN ATTEMPT ON THE GRAND, TETONS, WY",
                imageSrc = "https://amountainlifetime.net/img/gallery/41/1992/18/041_0621_92.jpg"
            ),
            CaptionedImage(
                caption = "EARL ALDERSON AND SUNRISE AT A WINTER CAMPSITE IN GARNET CANYON, TETONS, WY",
                imageSrc = "https://amountainlifetime.net/img/gallery/41/1992/18/041_0628_92.jpg"
            )
        )

        Assertions.assertEquals(expectedImages, extractor.extract(document))
    }
}