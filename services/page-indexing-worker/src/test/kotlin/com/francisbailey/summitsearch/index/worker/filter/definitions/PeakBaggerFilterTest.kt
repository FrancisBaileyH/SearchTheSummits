package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class PeakBaggerFilterTest: FilterTest() {

    @Test
    fun `skips expected links on page`() {
        verifyFilter(
            linkDiscoveryFilterService,
            expectedToSkip = listOf(
                "https://peakbagger.com/climber/ClimbListC.aspx?cid=10740",
                "https://peakbagger.com/search.aspx",
                "https://peakbagger.com/climber/report"
            ),
            expectedNotToSkip = listOf(
                "https://peakbagger.com/climber/ClimbListC.aspx?cid=10740&j=0",
                "https://peakbagger.com/climber/climber.aspx?cid=10740",
                "https://peakbagger.com/Peak.aspx?pid=5736",
                "https://peakbagger.com/range.aspx?rid=1",
                "https://peakbagger.com/climber/ascent.aspx?aid=1596578"
            )
        )
    }

    @Test
    fun `skips indexing expected links on page`() {
        verifyFilter(
            documentIndexingFilterService,
            expectedToSkip = listOf(
                "https://peakbagger.com/climber/ClimbListC.aspx?cid=10740",
                "https://peakbagger.com/search.aspx",
                "https://peakbagger.com/climber/report",
                "https://peakbagger.com/climber/ClimbListC.aspx?cid=10740&j=0",
                "https://peakbagger.com/climber/climber.aspx?cid=10740",
                "https://peakbagger.com/Peak.aspx?pid=5736",
                "https://peakbagger.com/range.aspx?rid=1",
            ),
            expectedNotToSkip = listOf(
                "https://peakbagger.com/climber/ascent.aspx?aid=1596578"
            )
        )
    }

}