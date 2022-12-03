package com.francisbailey.summitsearch.index.worker.extension

import org.jsoup.nodes.Element

fun Element.getLinks(): List<String> {
    return this.select("a[href]").map { it.attr("abs:href") }
}