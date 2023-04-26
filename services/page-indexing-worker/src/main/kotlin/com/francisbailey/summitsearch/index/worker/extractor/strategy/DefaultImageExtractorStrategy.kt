package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.*
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import org.jsoup.nodes.Document

class DefaultImageExtractorStrategy: ContentExtractorStrategy<List<CaptionedImage>> {
    override fun extract(document: Document): List<CaptionedImage> {
        return document.getWPCaptionedImages() +
            document.getFigCaptionedImages() +
            document.getDlCaptionedImages() +
            document.getBlogSpotCaptionedImages()
    }
}