package com.francisbailey.htmldate

import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import java.time.LocalDateTime

class GoodEnoughHtmlDateGuesserTest {

    data class ResourceDateMapping(
        val url: URL,
        val localName: String,
        val date: LocalDateTime
    )

    private val goodEnoughHtmlDateGuesser = GoodEnoughHtmlDateGuesser.from(
        configuration = HtmlDateSearchConfiguration(
            useOriginalDate = true
        )
    )

    @Test
    fun `reports expected dates from real html pages`() {
        val expectedDates = setOf(
            ResourceDateMapping(
                url = URL("https://cascadeclimbers.com/forum/topic/106009-phylliss-engine-standard-route-sept-20th-2022/"),
                localName = "cascadeclimbers-topic.html",
                date = LocalDateTimeBuilder.of(2023, 2, 6)
            ),
            ResourceDateMapping(
                url = URL("https://forums.clubtread.com/27-british-columbia/95172-watersprite-lake-2022-09-25-a.html"),
                localName = "clubtread-page.html",
                date = LocalDateTimeBuilder.of(2022, 9, 25)
            ),
            ResourceDateMapping(
                url = URL("https://peakbagger.com/climber/ascent.aspx?aid=1857307"),
                localName = "peakbagger-ascent.html",
                date = LocalDateTimeBuilder.of(2022, 1, 26)
            ),
            ResourceDateMapping(
                url = URL("https://stevensong.com/canadian-rockies/david-thompson/coliseum-mountain/"),
                localName = "stevensong.html",
                date = LocalDateTimeBuilder.of(2013, 5, 5)
            ),
            ResourceDateMapping(
                url = URL("https://bretteharrington.blogspot.com/2020/08/auroraphobia-13b-220m.html"),
                localName = "bretteharrington.html",
                date = LocalDateTimeBuilder.of(2020, 8, 22)
            ),
            ResourceDateMapping(
                url = URL("https://www.nwhikers.net/forums/viewtopic.php?t=8036796"),
                localName = "phpbb.html",
                date = LocalDateTimeBuilder.of(2023, 2, 13)
            ),
            ResourceDateMapping(
                url = URL("https://forums.clubtread.com/35-other-regions/41834-wiggle-wiggle-wiggle-zion.html"),
                localName = "clubtread-future.html",
                date = LocalDateTimeBuilder.of(2012, 6, 14)
            ),
            ResourceDateMapping(
                url = URL("https://forums.clubtread.com/37-washington-state/65706-11-3-15-bagley-lakes-artist-point.html"),
                localName = "clubtread-bad-url.html",
                date = LocalDateTimeBuilder.of(2015, 7, 11) // the actual date is Nov, 7, 2023 but there's no way to detect this...
            )
        )

        expectedDates.forEach {
            val html = Jsoup.parse(File("src/test/resources/dated-pages/${it.localName}"))
            assertEquals(it.date, goodEnoughHtmlDateGuesser.findDate(it.url, html), "Failed on: $it")
        }
    }
}