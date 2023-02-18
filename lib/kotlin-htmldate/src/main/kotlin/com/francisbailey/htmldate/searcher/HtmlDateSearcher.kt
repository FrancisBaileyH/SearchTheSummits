package com.francisbailey.htmldate.searcher

import org.jsoup.nodes.Document
import java.time.LocalDateTime

interface HtmlDateSearcher {
    fun getDateValue(document: Document): LocalDateTime?
}