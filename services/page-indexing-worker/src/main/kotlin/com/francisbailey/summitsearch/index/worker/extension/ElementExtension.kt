package com.francisbailey.summitsearch.index.worker.extension

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Some SEO Descriptions have HTML tags in them and could also contain
 * malicious script tags. Because we fetch these values with Jsoup.attr()
 * the values aren't stripped from HTML like our other .text() calls.
 */
fun Document.getSeoDescription(): String? {
    val description = this.selectFirst("meta[name=description]")
    return description?.attr("content")
}

fun Element.getLinks(): List<String> {
    return this.select("a[href]").map { it.attr("abs:href") }
}

fun Element.src(): String? {
    return this.attr("src")
}

fun Element.getOGImage(): CaptionedImage? {
    val imageUrl = this.selectFirst("meta[property=og:image]")?.attr("content")
    val imageCaption = this.selectFirst("meta[property=og:image:alt]")?.attr("content")

    return imageUrl?.let {
        CaptionedImage(
            imageSrc = imageUrl,
            caption = imageCaption ?: ""
        )
    }
}

fun Element.getCaptionedImages(parentSelector: String, captionSelector: String): List<CaptionedImage> {
    val figures = this.select(parentSelector) as List<Element>

    return figures.mapNotNull {
        val imageSrc = it.selectFirst("img[src~=(?i)\\.(png|jpe?g)]")?.src()
        val dataSrc = it.selectFirst("img[data-src~=(?i)\\.(png|jpe?g)]")?.attr("data-src")
        val caption = it.selectFirst(captionSelector)?.text()

        val image = imageSrc ?: dataSrc // fallback to data-src if it's present

        if (image.isNullOrBlank() || caption.isNullOrBlank()) {
            null
        } else {
            CaptionedImage(
                imageSrc = image,
                caption = caption
            )
        }
    }
}

fun Element.getWPCaptionedImages(): List<CaptionedImage> {
    return getCaptionedImages(".wp-caption", ".wp-caption-text")
}

fun Element.getFigCaptionedImages(): List<CaptionedImage> {
    return getCaptionedImages("figure", "figcaption")
}

fun Element.getDlCaptionedImages(): List<CaptionedImage> {
    return getCaptionedImages("dl", "dd")
}

data class CaptionedImage(
    val caption: String,
    val imageSrc: String
)