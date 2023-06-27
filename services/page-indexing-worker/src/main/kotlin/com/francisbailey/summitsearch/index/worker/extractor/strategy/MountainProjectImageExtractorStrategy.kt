package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extension.getCaptionedImages
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import org.jsoup.nodes.Document

class MountainProjectImageExtractorStrategy: ContentExtractorStrategy<List<CaptionedImage>> {

    override fun extract(document: Document): List<CaptionedImage> {
        return document.getCaptionedImages("div.card-with-photo", ".card-text .title-row")
    }
}