package com.francisbailey.summitsearch.index.worker.extension

import org.jsoup.nodes.Element

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
        val image = it.selectFirst("img[src~=(?i)\\.(png|jpe?g)]")?.src()
        val caption = it.selectFirst(captionSelector)?.text()

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