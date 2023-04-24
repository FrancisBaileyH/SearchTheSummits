package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extension.getDlCaptionedImages
import com.francisbailey.summitsearch.index.worker.extension.getFigCaptionedImages
import com.francisbailey.summitsearch.index.worker.extension.getWPCaptionedImages
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import org.jsoup.nodes.Document

class DefaultImageExtractorStrategy: ContentExtractorStrategy<List<CaptionedImage>> {
    override fun extract(document: Document): List<CaptionedImage> {
        return document.getWPCaptionedImages() + document.getFigCaptionedImages() + document.getDlCaptionedImages()
    }
}