package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import com.francisbailey.htmldate.extension.setOrOverrideIf
import org.jsoup.nodes.Document
import java.time.LocalDateTime

class HtmlTimeElementSearcher(
    private val configuration: HtmlDateSearchConfiguration,
    private val parser: HtmlDateParser
): HtmlDateSearcher {
    override fun getDateValue(document: Document): LocalDateTime? {
        val elements = document.getElementsByTag("time")

        if (elements.size > configuration.maxElementScan) {
            return null
        }

        var oldestDate: LocalDateTime? = null
        var newestDate: LocalDateTime? = null

        elements.forEach {
            val text = it.ownText().trim()
            val cssClass = it.classNames()
            val dateTime = it.attr("datetime").trim().lowercase()
            val pubDate = it.attr("pubdate").trim().lowercase()

            if (dateTime.length > 6) {
                val date = parser.parse(dateTime)

                when {
                    pubDate == "pubdate" ||
                    cssClass.contains("entry-date") ||
                    cssClass.contains("entry-time") -> {
                        if (date != null && configuration.useOriginalDate) {
                            return date
                        }
                    }
                    else -> {
                        oldestDate = oldestDate.setOrOverrideIf(date) { original, new -> new.isBefore(original) }
                        newestDate = newestDate.setOrOverrideIf(date) { original, new -> new.isAfter(original) }
                    }
                }
            }

            if (text.length > 6) {
                val date = parser.parse(text)

                oldestDate = oldestDate.setOrOverrideIf(date) { original, new -> new.isBefore(original) }
                newestDate = newestDate.setOrOverrideIf(date) { original, new -> new.isAfter(original) }
            }
        }

        return if (configuration.useOriginalDate) {
            oldestDate
        } else {
            newestDate
        }
    }
}