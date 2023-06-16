package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.getSeoDescription
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import org.springframework.stereotype.Component

@Component
class DefaultContentExtractorStrategy: ContentExtractorStrategy<DocumentText> {

    override fun extract(document: Document): DocumentText {
        document.body().select(EXCLUDED_TAG_EVALUATOR).forEach {
            it.remove()
        }

        return DocumentText(
            title = document.title(),
            description = document.getSeoDescription() ?: "",
            semanticText = document.body().select("p").text(),
            rawText = document.body().text()
        )
    }

    companion object {
        private val EXCLUDED_TAG_EVALUATOR = object: Evaluator() {
            private val excludedTags = setOf("ul", "li", "a", "nav", "footer", "header", "form", "select")

            override fun matches(root: Element, element: Element): Boolean {
                return excludedTags.contains(element.normalName())
            }
        }
    }
}