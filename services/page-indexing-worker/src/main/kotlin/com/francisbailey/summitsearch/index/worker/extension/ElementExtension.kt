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

fun Element.getCaptionedImages(): List<CaptionedImage> {
    val figures = this.select("figure") as List<Element>

    return figures.mapNotNull {
        val image = it.selectFirst("img[src~=(?i)\\.(png|jpe?g)]")?.src()
        val caption = it.selectFirst("figcaption")?.text()

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
    val captions = this.select(".wp-caption")

    return captions.mapNotNull {
        val image = it.selectFirst("img[src~=(?i)\\.(png|jpe?g)]")?.src()
        val caption = it.selectFirst(".wp-caption-text")?.text()

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

data class CaptionedImage(
    val caption: String,
    val imageSrc: String
)