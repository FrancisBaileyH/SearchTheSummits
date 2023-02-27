package com.francisbailey.htmldate

import com.francisbailey.htmldate.extractor.*
import com.francisbailey.htmldate.searcher.*
import org.jsoup.nodes.Document
import java.net.URL
import java.time.LocalDateTime


enum class HtmlDateSearchType {
    STANDARD
}

class HtmlDateSearchConfiguration(
    val searchType: HtmlDateSearchType = HtmlDateSearchType.STANDARD,
    val useOriginalDate: Boolean,
    val maxElementScan: Int = 150
)


class GoodEnoughHtmlDateGuesser {
    private val configuration: HtmlDateSearchConfiguration
    private val urlExtractor: DateExtractorStrategy
    private val dateValueSearchers: List<HtmlDateSearcher>

    internal constructor(
        urlExtractor: DateExtractorStrategy,
        searchers: List<HtmlDateSearcher>,
        configuration: HtmlDateSearchConfiguration
    ) {
        this.configuration = configuration
        this.urlExtractor = urlExtractor
        this.dateValueSearchers = searchers
    }

    fun findDate(source: URL, document: Document): LocalDateTime? {
        val currentDate = LocalDateTime.now()
        val urlDate = urlExtractor.find(source.toString())

        if (urlDate != null && urlDate < currentDate) {
            return urlDate
        }

        return dateValueSearchers.firstNotNullOfOrNull { searcher ->
            searcher.getDateValue(document)?.let {
                if (it > currentDate) {
                    null
                } else {
                    it
                }
            }
        }
    }

    companion object {
        fun from(configuration: HtmlDateSearchConfiguration): GoodEnoughHtmlDateGuesser {
            val parser = HtmlDateParser(
                extractors = listOf(
                    NoRegexYMDNoSeparatorDateExtractor(),
                    YYYYMMDDNoSeparatorDateExtractor(),
                    YMDDateExtractor(),
                    DMYDateExtractor(),
                    MDYDateExtractor(),
                    YMDateExtractor(),
                    MYDateExtractor(),
                    LongMDYDateExtractor(),
                    LongDMYDateExtractor()
                )
            )

            return GoodEnoughHtmlDateGuesser(
                configuration = configuration,
                searchers = listOf(
                    HtmlMetaSearcher(configuration, parser),
                    HtmlAbbrSearcher(configuration, parser),
                    HtmlExtendedAbbrSearcher(configuration, parser),
                    HtmlDateElementSearcher(configuration, parser),
                    HtmlTimeElementSearcher(configuration, parser),
                    HtmlTitleSearcher(parser),
                    CommonForumSearcher(parser)
                ),
                urlExtractor = UrlDateExtractor()
            )
        }
    }


}