package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import org.jsoup.nodes.Document
import java.time.LocalDateTime

class CommonForumSearcher(
    private val parser: HtmlDateParser
): HtmlDateSearcher {

    override fun getDateValue(document: Document): LocalDateTime? {
        val postDateElement = document.select("div.postdetails").firstOrNull {
            !it.text().lowercase().contains("joined")
        }

        val dateCreatedSpan = document.selectFirst("span[itemprop=\"dateCreated\"]")

        val searchableElements = listOf(
            postDateElement,
            dateCreatedSpan
        )

        return searchableElements.firstNotNullOfOrNull {
            it?.let {
                parser.parse(it.text())
            }
        }
    }
}