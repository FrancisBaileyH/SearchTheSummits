package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class HtmlDateElementSearcher(
    configuration: HtmlDateSearchConfiguration,
    parser: HtmlDateParser
): HtmlExtendedElementSearcher(configuration, parser) {
    override fun selectExtendedElements(document: Document): List<Element> {
        return document.select("p, div, li, span")
            .filter { it: Element ->
                val hasDateClass = it.classNames().any { cssClass ->
                    cssClass in HtmlDateAttributes.DATE_CLASS_INDICATORS
                }

                val hasDateId = it.id() in HtmlDateAttributes.DATE_ID_INDICATORS

                hasDateClass || hasDateId
            }
    }
}