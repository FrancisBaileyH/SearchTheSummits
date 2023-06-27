package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MountainProjectContextExtractorStrategyTest {

    @Test
    fun `extracts expected content from mountain project route page`() {
        val expectedParagraphContent = "A mix of quality rock, icy corners, and exposed snow. The crux is in the first pitch where corner systems don't get a great deal of sun and can be slippery even in good conditions. The final pitch features excellent pockets on an incredible arete with great exposure. Conditions on this route are subject to variance through the season and across different years."
        val expectedSeoDescription = "Climb Cara Este on Aguja de I'S"
        val document = loadHtml("mountainproject/route.html")

        val strategy = MountainProjectContentExtractorStrategy()

        val documentContent = strategy.extract(document)

        Assertions.assertEquals(documentContent.semanticText, expectedParagraphContent)
        Assertions.assertEquals(documentContent.description, expectedSeoDescription)
        Assertions.assertEquals(documentContent.title, "Rock Climb Cara Este, Santa Cruz")
    }

    @Test
    fun `extracts expected title when it contains route name`() {
        val expectedParagraphContent = "Dubbed the best ice climb in Patagonia, it could potential be the best ice and mixed climb in the world. It is much more complex and longer than the guidebook would suggest. Some of the complexities depend on the condition of the climb and the season. Some arise from the remote location - there is much more to it than the rating. Below is the description and suggested itinerary for someone fit, technically prepared and hoping to complete the route. Location Located on the West side of Cerro Torre and usually approached from the town of Chalten over two days. The usual approach is through Niponino and col Standhardt and takes most competent parties two days, although can be done in a long day by the fittest of the fit. Helps to have the gear cached in Niponino. Protection Ice screws, cams to BD #2, picket(s), ice tools, crampons, wings, v-thread tool, stuff sacks, two 60M ropes."
        val expectedSeoDescription = "Cerro Torre - Via dei Ragni"
        val document = loadHtml("mountainproject/route-title-alt.html")

        val strategy = MountainProjectContentExtractorStrategy()

        val documentContent = strategy.extract(document)

        Assertions.assertEquals(expectedParagraphContent, documentContent.semanticText)
        Assertions.assertEquals(documentContent.description, expectedSeoDescription)
        Assertions.assertEquals(documentContent.title, "Climb Cerro Torre - Via dei Ragni, Santa Cruz")
    }


}