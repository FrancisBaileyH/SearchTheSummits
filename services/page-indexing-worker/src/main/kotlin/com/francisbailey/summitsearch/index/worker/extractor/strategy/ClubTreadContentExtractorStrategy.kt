package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.getSeoDescription
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import org.jsoup.nodes.Document

class ClubTreadContentExtractorStrategy: ContentExtractorStrategy<DocumentText> {
    override fun extract(document: Document): DocumentText {
        val forumContent = document.body().select(".main-column-text")
        val title = document.title()

        val rawText = if (forumContent.isNotEmpty()) {
            forumContent.text()
        } else {
            document.body().text()
        }

        return DocumentText(
            title = title,
            description = document.getSeoDescription() ?: "",
            rawText = rawText,
            semanticText = ""
        )
    }
}