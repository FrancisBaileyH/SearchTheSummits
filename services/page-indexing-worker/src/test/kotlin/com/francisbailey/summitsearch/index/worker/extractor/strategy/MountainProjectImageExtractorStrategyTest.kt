package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MountainProjectImageExtractorStrategyTest {

    @Test
    fun `extracts expected images`() {
        val document = loadHtml("mountainproject/route-title-alt.html")
        val extractor = MountainProjectImageExtractorStrategy()

        val expectedImages = listOf(
            CaptionedImage(
                caption = "Approaching the Standhardt Col",
                imageSrc = "https://mountainproject.com/assets/photos/climb/116538156_smallMed_1550338708_topo.jpg?cache=1678855915"
            ),
            CaptionedImage(
                caption = "The summit mushrooms",
                imageSrc = "https://mountainproject.com/assets/photos/climb/116537879_smallMed_1550317881.jpg?cache=1671253197"
            )
        )

        Assertions.assertEquals(expectedImages, extractor.extract(document))
    }

}