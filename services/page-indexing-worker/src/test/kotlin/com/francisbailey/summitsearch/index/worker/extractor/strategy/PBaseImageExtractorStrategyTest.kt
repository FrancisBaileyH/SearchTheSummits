package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PBaseImageExtractorStrategyTest {

    @Test
    fun `indexes expected content from pbase page`() {
        val document = loadHtml("pbase/image-page.html")

        val strategy = PBaseImageExtractorStrategy()

        val imageContent = strategy.extract(document).first()

        Assertions.assertEquals(imageContent.imageSrc, "https://a4.pbase.com/o6/20/434420/1/72292359.L3u3koLx.Waddington122806_0017.jpg")
        Assertions.assertEquals(imageContent.caption, "The Tiedemann Group, View SE (W122806--_0017.jpg) Combatant is seen at upper center. Tiedemann is visible in cloud, L background. Hickson's summit is in L foreground. Combatant Col & Waddington are at R.")
    }
}