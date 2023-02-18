package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class HtmlExtendedAbbrSearcher(
    configuration: HtmlDateSearchConfiguration,
    parser: HtmlDateParser
): HtmlExtendedElementSearcher(configuration, parser) {

    override fun selectExtendedElements(document: Document): List<Element> {
        return document.getElementsByTag("abbr")
    }
}