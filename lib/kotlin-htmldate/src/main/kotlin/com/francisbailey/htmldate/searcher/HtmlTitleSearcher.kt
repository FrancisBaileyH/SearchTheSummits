package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import org.jsoup.nodes.Document
import java.time.LocalDateTime

class HtmlTitleSearcher(
    private val parser: HtmlDateParser
): HtmlDateSearcher {

    override fun getDateValue(document: Document): LocalDateTime? {
        return document.getElementsByTag("h1").firstNotNullOfOrNull {
            val title = it.text().trim()
            parser.parse(title)
        }
    }
}