package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import org.jsoup.nodes.Document
import java.time.LocalDateTime

class HtmlAbbrSearcher(
    private val configuration: HtmlDateSearchConfiguration,
    private val parser: HtmlDateParser
): HtmlDateSearcher {

    override fun getDateValue(document: Document): LocalDateTime? {
        val elements = document.getElementsByTag("abbr")

        if (elements.size > configuration.maxElementScan) {
            return null
        }

        var oldestDate: LocalDateTime? = null
        var newestDate: LocalDateTime? = null

        elements.forEach { item ->
            val cssClass = item.attr("class").trim()
            val dataUTime = item.attr("data-utime").trim()

            when {
                dataUTime.isNotBlank() && dataUTime.toIntOrNull() != null -> {
                    parser.parse(dataUTime)?.let {
                        oldestDate = oldestDate?.coerceAtMost(it) ?: it
                        newestDate = newestDate?.coerceAtLeast(it) ?: it
                    }
                }
                cssClass.isNotBlank() && cssClass in HtmlDateAttributes.CLASS_ATTRIBUTE_KEYS -> {
                    val text = item.ownText().trim()
                    val title = item.attr("title").trim()

                    if (title.isNotBlank()) {
                        parser.parse(title)?.let {
                            oldestDate = oldestDate?.coerceAtMost(it) ?: it
                            newestDate = newestDate?.coerceAtLeast(it) ?: it
                        }
                    }

                    if (text.length > 10) {
                        parser.parse(text)?.let {
                            oldestDate = oldestDate?.coerceAtMost(it) ?: it
                            newestDate = newestDate?.coerceAtLeast(it) ?: it
                        }
                    }
                }
            }
        }

        return if (configuration.useOriginalDate) {
            oldestDate
        } else {
            newestDate
        }
    }
}