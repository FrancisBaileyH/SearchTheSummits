package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import com.francisbailey.htmldate.extension.setOrOverrideIf
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
                    val date = parser.parse(dataUTime)
                    oldestDate = oldestDate.setOrOverrideIf(date) { original, new -> new.isBefore(original) }
                    newestDate = newestDate.setOrOverrideIf(date) { original, new -> new.isAfter(original) }
                }
                cssClass.isNotBlank() && cssClass in HtmlDateAttributes.CLASS_ATTRIBUTE_KEYS -> {
                    val text = item.ownText().trim()
                    val title = item.attr("title").trim()

                    if (title.isNotBlank()) {
                        val date = parser.parse(title)
                        oldestDate = oldestDate.setOrOverrideIf(date) { original, new -> new.isBefore(original) }
                        newestDate = newestDate.setOrOverrideIf(date) { original, new -> new.isAfter(original) }
                    }

                    if (text.length > 10) {
                        val date = parser.parse(text)
                        oldestDate = oldestDate.setOrOverrideIf(date) { original, new -> new.isBefore(original) }
                        newestDate = newestDate.setOrOverrideIf(date) { original, new -> new.isAfter(original) }
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