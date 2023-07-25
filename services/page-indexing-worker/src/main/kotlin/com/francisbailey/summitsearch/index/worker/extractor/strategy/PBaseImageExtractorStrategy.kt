package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import org.jsoup.nodes.Document

class PBaseImageExtractorStrategy: ContentExtractorStrategy<List<CaptionedImage>> {

    /**
     * When viewing source of the page from a browser the full size image is returned. However,
     * programmatically fetching the image page serves up the smallest image first. So, we'll search for
     * the other sizes and select the original as the image to use
     */
    override fun extract(document: Document): List<CaptionedImage> {
        val imageSrc  = document
            .select("#othersizes a").firstOrNull {
                it.attr("imgsize") == "original"
            }?.attr("imgurl")

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