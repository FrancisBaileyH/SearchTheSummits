package com.francisbailey.summitsearch.indexservice.extension

import org.jsoup.nodes.Document

/**
 * Some SEO Descriptions have HTML tags in them and could also contain
 * malicious script tags. Because we fetch these values with Jsoup.attr()
 * the values aren't stripped from HTML like our other .text() calls.
 */
fun Document.getSeoDescription(): String? {
    val description = this.selectFirst("meta[name=description]")
    return description?.attr("content")
}