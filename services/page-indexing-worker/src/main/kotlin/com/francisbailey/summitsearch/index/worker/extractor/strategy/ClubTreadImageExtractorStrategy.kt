package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extension.src
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import org.jsoup.nodes.Document

class ClubTreadImageExtractorStrategy: ContentExtractorStrategy<List<CaptionedImage>> {

    override fun extract(document: Document): List<CaptionedImage> {
        val forumContent = document.body().select(".main-column-text")
        val images = forumContent.select("img[src~=.*(?:forumPix|attachment.php).*]")

        return images.mapNotNull {
            it.src()
        }.map {
            CaptionedImage(
                caption = "",
                imageSrc = it
            )
        }
    }
}