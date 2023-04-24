package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.loadHtml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClubTreadImageExtractorStrategyTest {

    private val extractor = ClubTreadImageExtractorStrategy()

    @Test
    fun `extracts expected images`() {
        val document = loadHtml("clubtread/forum-post.html")

        val expectedImages = listOf(
            CaptionedImage(
                caption = "",
                imageSrc = "https://www.clubtread.com/forumPix/402000/402649.jpg"
            ),
            CaptionedImage(
                caption = "",
                imageSrc = "https://forums.clubtread.com/attachment.php?attachmentid=297251&thumb=1&d=1680829407"
            )
        )

        assertEquals(expectedImages, extractor.extract(document))
    }
}