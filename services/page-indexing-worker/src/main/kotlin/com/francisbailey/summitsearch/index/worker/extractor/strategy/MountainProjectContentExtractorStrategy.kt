package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.getSeoDescription
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import org.jsoup.nodes.Document

class MountainProjectContentExtractorStrategy: ContentExtractorStrategy<DocumentText> {

    override fun extract(document: Document): DocumentText {
        val title = document.title()
        val routeName = document.select("h1").text()
        val routeDetails = document.select("h2")
            .firstOrNull { it.text() == "Description" }
            ?.siblingElements()
            ?.text()

        val parentAreaName = document
            .select("#route-page a[href=https://www.mountainproject.com/route-guide]")
            .first()
            ?.lastElementSibling()
            ?.text()

        val seoDescription = if (!parentAreaName.isNullOrBlank()) {
            if (!routeName.contains(parentAreaName)) {
                "Climb $routeName on $parentAreaName"
            } else {
                routeName
            }
        } else {
            document.getSeoDescription()
        }

        val semanticText = if (!routeDetails.isNullOrBlank()) {
            routeDetails
        } else {
            document.body().text()
        }

        return DocumentText(
            title = title,
            description = seoDescription ?: "",
            semanticText = semanticText,
            rawText = ""
        )
    }
}