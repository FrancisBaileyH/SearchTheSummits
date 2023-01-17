package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.configuration.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import java.net.URL
import java.util.regex.Pattern

class CrawlerFilterTest {

    @Test
    fun `default index filter skips home pages`() {
        val expectedToSkip = listOf(
            "https://www.example.com/",
            "https://www.example.com",
            "https://www.example.com/index.html",
            "https://www.example.com/page/1",
            "https://www.example.com/page/1/",
            "https://www.example.com/nested-page/page/1/"
        )

        val expectedNotToSkip = listOf(
            "https://www.example.com/page/1/some-article",
            "https://www.example.com/2015/08/04/primitive-canada/"
        )

        verifyFilter(DefaultIndexFilterChain, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `sverdina filter skips short pages and main page`() {
        val expectedToSkip = listOf(
            "http://sverdina.com/middle_sister1_SHORT.asp",
            "http://sverdina.com/climb.asp"
        )

        val expectedNotToSkip = listOf(
            "http://sverdina.com/middle_sister1.asp"
        )

        verifyFilter(SVerdinaIndexFilterChain, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `default filter skips wordpress uploads and page images`() {
        val urlString = "https://francisbaileyh.com"

        val expectedToSkip = listOf(
            "/wp-content/test",
            "/author/francis",
            "/tag/some-tag",
            "/category/some-category",
            "/some-file.jpg",
            "/some-file.gif",
            "/some-file.jpeg",
            "/some-file.png",
            "/some-file.jPeG",
            "/some-file?query=x",
            "/2022",
            "/2022/",
            "/2022/06",
            "/2022/06/",
            "/2022/06/12",
            "/2022/06/12/",
            "/blog/2022/06/12/",
            "/search/test",
            "/search/label/skiing",
            "/blog/tag/test",
            "/blog/category/test",
            "/blog/author/test",
            "/blog/wp-content/test"
        ).map {
            urlString + it
        }

        val expectedNotToSkip = listOf(
            "/some-normal-page",
            "/2022/06/12/some-page"
        ).map {
            urlString + it
        }

        verifyFilter(DefaultFilterChain, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `cascade climbers filter skips expected links`() {
        val expectedToSkip = listOf(
            "https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010/?do=getLastComment",
            "https://cascadeclimbers.com/forum/topic/75809-joffre-peak-flavelle-lane-8152010/",
            "https://cascadeclimbers.com/forum/profile/9797-jasong/",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/?sortby=title&sortdirection=asc&page=4"
        )

        val expectedNotToSkip = mutableListOf(
            "https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010/",
            "https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/page/2/",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/page/2",
            "https://cascadeclimbers.com/forum/forum/16-british-columbiacanada/page/20",
            "https://cascadeclimbers.com/forum/topic/53522-tr-mt-queen-bess-se-buttress-good-queen-bes/"
        )

        val allowedTopics = listOf(
            "34-alaska",
            "16-british-columbiacanada",
            "10-north-cascades",
            "11-alpine-lakes",
            "13-southern-wa-cascades",
            "12-mount-rainier-np",
            "14-olympic-peninsula",
            "28-centraleastern-washington",
            "15-oregon-cascades",
            "41-columbia-river-gorge",
            "25-california",
            "49-idaho",
            "48-montana",
            "52-the-rest-of-the-us-and-international"
        ).map {
            "https://cascadeclimbers.com/forum/forum/$it/"
        }

        expectedNotToSkip.addAll(allowedTopics)

        verifyFilter(CascadeClimbersFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `club tread filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "27-british-columbia",
            "130-canadian-rockies",
            "34-alberta",
            "37-washington-state",
            "35-other-regions"
        ).map {
            "https://forums.clubtread.com/$it/"
        }.toMutableList()

        expectedNotToSkip.add("https://forums.clubtread.com/27-british-columbia/95172-watersprite-lake-2022-09-25-a.html")
        expectedNotToSkip.add("https://forums.clubtread.com/27-british-columbia/40728-ain-t-elfin-lakes-cayoosh-creek-hut.html")
        expectedNotToSkip.add("https://forums.clubtread.com/130-canadian-rockies/index2.html") // paginated topic
        expectedNotToSkip.add("https://forums.clubtread.com/130-canadian-rockies/index20.html") // paginated topic
        expectedNotToSkip.add("https://forums.clubtread.com/27-british-columbia/43281-t-tower-t-tiara.html")

        val expectedToSkip = listOf(
            "https://forums.clubtread.com/27-british-columbia/40728-ain-t-elfin-lakes-cayoosh-creek-hut-2.html",
            "https://forums.clubtread.com/newreply.php?do=newreply&p=476682",
            "https://forums.clubtread.com/members/71772-losthiker.html",
            "https://forums.clubtread.com/27-british-columbia/95172-watersprite-lake-2022-09-25-a-print.html",
            "https://forums.clubtread.com/27-british-columbia/44397-corral-trail-memorial-lookout-mar-03-13-a-prev-thread.html",
            "https://forums.clubtread.com/27-british-columbia/44397-corral-trail-memorial-lookout-mar-03-13-a-next-thread.html"
        )

        verifyFilter(ClubTreadFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `voc filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.ubc-voc.com/2022/07/23/duffey-double-crown",
            "https://www.ubc-voc.com/2022/11/02/climbing-skywalker-5-8-in-memory-of-will-ungar"
        )


        val expectedToSkip = listOf(
            "https://www.ubc-voc.com/tag/first-snow",
            "https://www.ubc-voc.com/phorum5/index.php",
            "https://www.ubc-voc.com/gallery/main.php?g2_itemId=613635",
            "https://www.ubc-voc.com/category/trip-reports/cycle,paddle",
            "https://www.ubc-voc.com/wiki/Main_Page"
        )

        verifyFilter(UBCVarsityOutdoorClubFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `drdirtbag skips expected links`() {
        val expectedToSkip = listOf(
            "https://www.drdirtbag.com/2015/08/04/primitive-canada/primitive-canada-2/"
        )

        val expectedNotToSkip = listOf(
            "https://www.drdirtbag.com/2015/08/04/primitive-canada/"
        )

        verifyFilter(DrDirtbagFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `american alpine journal skips expected links on indexing`() {
        val expectedToSkip = listOf(
            "https://publications.americanalpineclub.org/test",
            "https://publications.americanalpineclub.org/articles?page=4"
        )

        val expectedNotToSkip = listOf(
            "https://publications.americanalpineclub.org/articles/12196347903",
            "https://publications.americanalpineclub.org/articles/12196347903/"
        )

        verifyFilter(AmericanAlpineJournalIndexFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `american alpine journal skips expected links on discovery`() {
        val expectedToSkip = listOf(
            "https://publications.americanalpineclub.org/test"
        )

        val expectedNotToSkip = listOf(
            "https://publications.americanalpineclub.org/articles/12196347903",
            "https://publications.americanalpineclub.org/articles/12196347903/",
            "https://publications.americanalpineclub.org/articles?page=40"
        )

        verifyFilter(AmericanAlpineJournalFilter, expectedToSkip, expectedNotToSkip)
    }

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

        verifyFilter(RockyMountainSummitsFilter, expectedToSkip, expectedNotToSkip)
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

        verifyFilter(RockyMountainSummitsIndexFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `idaho climbing guides index filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.idahoaclimbingguide.com/bookupdates/peak-7623-by-mike-hays/",
            "https://www.idahoaclimbingguide.com/bookupdates/peak-7623-by-mike-hays"
        )

        val expectedToSkip = listOf(
            "https://www.idahoaclimbingguide.com/wp-content/uploads/PICT000144-350x370.jpg"
        )

        verifyFilter(IdahoClimbingGuideIndexFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `idaho climbing guides filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.idahoaclimbingguide.com/peak-lists/",
            "https://www.idahoaclimbingguide.com/peak-index/?fcoby=elevd&alpha=0&ecomp=6000"
        )

        val expectedToSkip = listOf(
            "https://www.idahoaclimbingguide.com/wp-content/uploads/PICT000144-350x370.jpg"
        )

        verifyFilter(IdahoClimbingGuideFilter, expectedToSkip, expectedNotToSkip)
    }

    // https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4
    @Test
    fun `mef org index filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4",
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4/"
        )

        val expectedToSkip = listOf(
            "https://www.mef.org.uk/expeditions/p4",
            "https://www.mef.org.uk/expeditions/p401"
        )

        verifyFilter(MountEverestFoundationIndexFilter, expectedToSkip, expectedNotToSkip)
    }

    @Test
    fun `mef org filter skips expected links`() {
        val expectedNotToSkip = listOf(
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4",
            "https://www.mef.org.uk/expeditions/british-western-hajar-traverse-2003-4/",
            "https://www.mef.org.uk/expeditions/p400"
        )

        val expectedToSkip = listOf(
            "https://www.mef.org.uk",
            "https://www.mef.org.uk/about"
        )

        verifyFilter(MountEverestFoundationFilter, expectedToSkip, expectedNotToSkip)
    }


    @Test
    fun `runs inclusive filter chain associated with host`() {
        val filterService = DocumentFilterService(DefaultFilterChain)
        val path = "/include/this/path/only"
        val host = "www.francisbaileyh.com"

        val inclusiveChain = DocumentFilterChain(exclusive = false).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^$path$")))
        }

        val url = URL("https://$host")
        filterService.addFilterChain(url, inclusiveChain)

        assertFalse(filterService.shouldFilter(URL("https://$host$path")))
        assertTrue(filterService.shouldFilter(URL("https://$host/include")))
    }

    @Test
    fun `runs exclusive filter chain associated with host`() {
        val filterService = DocumentFilterService(DefaultFilterChain)
        val path = "/include/this/path/only"
        val host = "www.francisbaileyh.com"

        val inclusiveChain = DocumentFilterChain(exclusive = true).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^$path$")))
        }

        val url = URL("https://$host")
        filterService.addFilterChain(url, inclusiveChain)

        assertTrue(filterService.shouldFilter(URL("https://$host$path")))
        assertFalse(filterService.shouldFilter(URL("https://$host/include")))
    }

    @Test
    fun `does not run default chain if chain exists for host`() {
        val defaultChain = mock<DocumentFilterChain>()
        val filterService = DocumentFilterService(defaultChain)

        val inclusiveChain = DocumentFilterChain(exclusive = false).apply {
            addFilter(PathMatchingDocumentFilter(Pattern.compile(".*")))
        }

        filterService.addFilterChain(URL("https://francisbaileyh.com"), inclusiveChain)
        filterService.shouldFilter(URL("https://francisbaileyh.com/test"))

        verifyNoInteractions(defaultChain)
    }

    @Test
    fun `merge adds rules to chain`() {
        val url = URL("https://francisbailey.com/test")
        val chain = DocumentFilterChain(exclusive = true)

        assertFalse(chain.shouldFilter(url))

        val chainToMerge = DocumentFilterChain(exclusive = true)
        chainToMerge.addFilter(PathMatchingDocumentFilter(Pattern.compile("^/test$")))

        chain.merge(chainToMerge)

        assertTrue(chain.shouldFilter(url))
    }


    private fun verifyFilter(filter: DocumentFilterChain, expectedToSkip: List<String>, expectedNotToSkip: List<String>) {
        expectedToSkip.forEach {
            assertTrue(filter.shouldFilter(URL(it)))
        }

        expectedNotToSkip.forEach {
            assertFalse(filter.shouldFilter(URL(it)))
        }
    }

}