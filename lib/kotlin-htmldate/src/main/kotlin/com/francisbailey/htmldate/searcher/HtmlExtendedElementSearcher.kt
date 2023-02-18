package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import com.francisbailey.htmldate.extractor.DateExtractorPatterns
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.LocalDateTime

abstract class HtmlExtendedElementSearcher(
    private val configuration: HtmlDateSearchConfiguration,
    private val parser: HtmlDateParser
): HtmlDateSearcher {

    internal abstract fun selectExtendedElements(document: Document): List<Element>

    override fun getDateValue(document: Document): LocalDateTime? {
        val elements = selectExtendedElements(document)

        if (elements.size > configuration.maxElementScan) {
            return null
        }

        elements.forEach {
            val textContent = it.ownText().trim()
            val title = it.attr("title").trim()

            if (textContent.length > 6) {
                val toExamine = textContent
                    .take(48)
                    .replace(DateExtractorPatterns.LAST_NON_DIGITS.toRegex(), "")

                parser.parse(toExamine)?.let { date ->
                    return date
                }
            }

            if (title.isNotBlank()){
                val toExamine = title
                    .take(48)
                    .replace(DateExtractorPatterns.LAST_NON_DIGITS.toRegex(), "")

                parser.parse(toExamine)?.let { date ->
                    return date
                }
            }
        }


        return null
    }
}