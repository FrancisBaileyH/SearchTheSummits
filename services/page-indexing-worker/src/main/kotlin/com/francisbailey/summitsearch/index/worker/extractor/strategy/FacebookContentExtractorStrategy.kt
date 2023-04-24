package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import org.jsoup.nodes.Document


class FacebookContentExtractorStrategy: ContentExtractorStrategy<DocumentText> {

    override fun extract(document: Document): DocumentText {
        return DocumentText(
            title = document.title(),
            description = document.selectFirst("meta[property=og:description]")?.attr("content") ?: "",
            semanticText = document.selectFirst("meta[property=og:image:alt]")?.attr("content") ?: "",
            rawText = ""
        )
    }
}