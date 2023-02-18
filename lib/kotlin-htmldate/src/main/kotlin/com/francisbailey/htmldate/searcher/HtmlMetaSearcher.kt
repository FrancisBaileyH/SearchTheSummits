package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.LocalDateTime


class HtmlMetaSearcher(
    private val configuration: HtmlDateSearchConfiguration,
    private val parser: HtmlDateParser
): HtmlDateSearcher {

    override fun getDateValue(document: Document): LocalDateTime? {
        val metaDataElements = document.select("meta")

        return metaDataElements.firstNotNullOfOrNull {
            getDateValueFromMetaElement(it)?.let(parser::parse)
        }
    }

    private fun getDateValueFromMetaElement(element: Element): String? {
        val name =  element.attr("name").lowercase()
        val content = element.attr("content").lowercase()
        val property = element.attr("property").lowercase()
        val pubDate = element.attr("pubdate").lowercase()
        val itemProp = element.attr("itemprop").lowercase()
        val dateTime = element.attr("datetime").lowercase()

        if (content.isBlank() && dateTime.isBlank()) {
            return null
        }

        return when {
            property.isNotBlank() -> {
                when {
                    property in HtmlDateAttributes.PROPERTY_MODIFIED_ATTRIBUTES && !configuration.useOriginalDate ->
                        content
                    property in HtmlDateAttributes.DATE_ATTRIBUTES ->
                        content
                    else -> null
                }
            }
            name.isNotBlank() -> {
                when {
                    name in HtmlDateAttributes.DATE_ATTRIBUTES ->
                        content
                    name in HtmlDateAttributes.MODIFIED_ATTRIBUTE_KEYS && !configuration.useOriginalDate ->
                        content
                    else -> null
                }
            }
            pubDate.isNotBlank() -> {
                content
            }
            itemProp in HtmlDateAttributes.ITEM_PROPERTY_ATTRIBUTE_KEYS -> {
                when {
                    itemProp in HtmlDateAttributes.ITEM_PROPERTY_MODIFIED && configuration.useOriginalDate -> null
                    itemProp in HtmlDateAttributes.ITEM_PROPERTY_ORIGINAL && !configuration.useOriginalDate -> null
                    dateTime.isNotBlank() -> dateTime
                    content.isNotBlank() -> content
                    else -> null
                }
            }
            else -> null
        }
    }
}