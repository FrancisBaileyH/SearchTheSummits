package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extension.src
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import org.jsoup.nodes.Document

class PBaseImageExtractorStrategy: ContentExtractorStrategy<List<CaptionedImage>> {

    override fun extract(document: Document): List<CaptionedImage> {
        val imageSrc = document.select("#imgdiv img").first()?.src()
        val captionContainer = document.select("#imageinfo")

        val captionTitle = captionContainer.select("h3.title").text()
        val captionText = captionContainer.select("#imagecaption").text()

        val caption = "$captionTitle $captionText"

        return if (caption.isNotBlank() && !imageSrc.isNullOrBlank()) {
            listOf(CaptionedImage(imageSrc = imageSrc, caption =  caption))
        } else {
            emptyList()
        }
    }
}