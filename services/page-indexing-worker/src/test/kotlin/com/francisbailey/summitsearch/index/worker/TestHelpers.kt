package com.francisbailey.summitsearch.index.worker

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File

private val resources = File("src/test/resources")

fun loadHtml(name: String): Document {
    return Jsoup.parse(resources.resolve(name).readText())
}