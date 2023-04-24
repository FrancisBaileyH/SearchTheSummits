package com.francisbailey.summitsearch.index.worker.extractor

import org.jsoup.nodes.Document
import java.net.URL

data class DocumentText(
    val title: String,
    val description: String,
    val semanticText: String,
    val rawText: String,
)

interface ContentExtractorStrategy<T> {
    fun extract(document: Document): T
}

class ContentExtractor<T>(
    private val defaultExtractor: ContentExtractorStrategy<T>,
    private val extractorStrategyMap: Map<String, ContentExtractorStrategy<T>>
) {
    fun extract(url: URL, document: Document): T {
        val strategy = extractorStrategyMap[url.host] ?: defaultExtractor
        return strategy.extract(document)
    }
}