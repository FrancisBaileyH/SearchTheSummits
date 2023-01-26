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

    return figures.map {
        val image = it.select("img[src~=(?i)\\.(png|jpe?g)]").first()
        val caption = it.select("figcaption").text()
        val src = image?.src()

        src to caption
    }.filterNot {
        it.first.isNullOrBlank() || it.second.isNullOrBlank()
    }.map {
        CaptionedImage(
            caption = it.second,
            imageSrc = it.first!!
        )
    }
}

data class CaptionedImage(
    val caption: String,
    val imageSrc: String
)