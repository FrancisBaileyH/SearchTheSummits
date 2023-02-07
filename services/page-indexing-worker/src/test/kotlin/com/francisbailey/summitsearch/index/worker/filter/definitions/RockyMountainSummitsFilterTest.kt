package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class RockyMountainSummitsFilterTest: FilterTest() {

    @Test
    fun `rocky mountain summits filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://rockymountainsummits.com/co_spring_break_05/co2005.htm#snik",
            "https://rockymountainsummits.com/trip_reports/trip_report.php?trip_id=285",
            "https://rockymountainsummits.com/whitney/whitney.htm",
            "https://rockymountainsummits.com/trip_reports/trip_report.php?trip_id=52",
            "https://rockymountainsummits.com/trip_report.php?trip_id=115"
        )

        val expectedToSkip = listOf(
            "http://rockymountainsummits.com/idaho_best.htm",
            "https://rockymountainsummits.com/year_recap.php?year=2022"
        )

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `rocky mountain summits index filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://rockymountainsummits.com/co_spring_break_05/co2005.htm#snik",
            "https://rockymountainsummits.com/trip_reports/trip_report.php?trip_id=285",
            "https://rockymountainsummits.com/whitney/whitney.htm",
            "https://rockymountainsummits.com/trip_reports/trip_report.php?trip_id=52",
            "https://rockymountainsummits.com/trip_report.php?trip_id=115"
        )

        val expectedToSkip = listOf(
            "http://rockymountainsummits.com/idaho_best.htm",
            "https://rockymountainsummits.com/year_recap.php?year=2022"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }

}