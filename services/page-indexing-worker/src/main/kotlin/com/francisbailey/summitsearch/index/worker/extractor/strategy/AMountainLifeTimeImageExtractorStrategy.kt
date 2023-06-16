package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extension.getCaptionedImages
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import org.jsoup.nodes.Document

class AMountainLifeTimeImageExtractorStrategy: ContentExtractorStrategy<List<CaptionedImage>> {
    override fun extract(document: Document): List<CaptionedImage> {
        val imageCandidates = document.select(".lightbox .item")

        return document.getCaptionedImages(".lightbox .item", "p")
    }
}