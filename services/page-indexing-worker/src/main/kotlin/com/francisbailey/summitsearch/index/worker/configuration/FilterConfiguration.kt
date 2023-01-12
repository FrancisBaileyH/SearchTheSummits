package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.task.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.task.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.task.PathMatchingDocumentFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL
import java.util.regex.Pattern

@Configuration
open class FilterConfiguration {

    @Bean
    open fun linkDiscoveryFilterService(): DocumentFilterService {
        return DocumentFilterService(defaultChain = DefaultFilterChain).apply {
            addFilterChain(URL("https://cascadeclimbers.com"), CascadeClimbersFilter)
            addFilterChain(URL("https://forums.clubtread.com"), ClubTreadFilter)
            addFilterChain(URL("https://www.ubc-voc.com"), UBCVarsityOutdoorClubFilter)
            addFilterChain(URL("https://www.drdirtbag.com"), DrDirtbagFilter)
            addFilterChain(URL("https://publications.americanalpineclub.org"), AmericanAlpineJournalFilter)
            addFilterChain(URL("https://rockymountainsummits.com"), RockyMountainSummitsFilter)
            addFilterChain(URL("https://www.idahoaclimbingguide.com"), IdahoClimbingGuideFilter)
        }
    }

    @Bean
    open fun documentIndexingFilterService(): DocumentFilterService {
        return DocumentFilterService(defaultChain = DefaultIndexFilterChain).apply {
            addFilterChain(URL("http://sverdina.com"), SVerdinaIndexFilterChain)
            addFilterChain(URL("https://publications.americanalpineclub.org"), AmericanAlpineJournalIndexFilter)
            addFilterChain(URL("https://rockymountainsummits.com"), RockyMountainSummitsIndexFilter)
            addFilterChain(URL("https://www.idahoaclimbingguide.com"), IdahoClimbingGuideIndexFilter)
        }
    }
}

object DefaultIndexFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        // Skip indexing home pages as they're typically filled will previews or full feeds of the article
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^(?:/|/index.html|)$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/page/[0-9]{1,5}(?:/|)$")))
    }
}

object SVerdinaIndexFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climb\\.asp.*$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*_SHORT\\.asp.*$")))
    }
}

object WordPressFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/wp-content/.*" )))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/author/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/tag/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/category/.*")))
    }
}

object BlogSpotFilterchain: DocumentFilterChain(exclusive = true) {
    init {
        // Exclude Blogspot archives e.g. /2022 or /2022/12 or /2022/10/12
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/(?:[0-9]{4}|[0-9]{4}/[0-9]{2}|[0-9]{4}/[0-9]{2}/[0-9]{2})(?:/|)\$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/search/.*$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/feeds/.*$")))
    }
}

object ImageFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile(".*(?:jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE)))
    }
}

object DefaultFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        // Exclude Blogspot archives e.g. /2022 or /2022/12 or /2022/10/12
        merge(BlogSpotFilterchain)
        // Exclude query parameters by default
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/.*[?].*")))
        // Wordpress filters
        merge(WordPressFilterChain)
        // Exclude images
        merge(ImageFilterChain)
    }
}

/**
 * Allowed:
 * /forum/forum/16-british-columbiacanada/?page=4
 * /forum/forum/16-british-columbiacanada/
 * /forum/topic/99505-tr-angle-of-the-dangle/
 *
 * Not Allowed:
 * https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010/?do=getLastComment
 * /forum/forum/16-british-columbiacanada/?sortby=title&sortdirection=asc&page=4
 */
object CascadeClimbersFilter: DocumentFilterChain(exclusive = false) {
    init {
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
        )
        allowedTopics.forEach {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forum/forum/$it(?:|/|/page/[0-9]{1,10}/?|)$")))
        }
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forum/topic/[0-9]{1,7}-tr-[a-z0-9-]{1,250}(?:|/)$")))
    }
}

object ClubTreadFilter: DocumentFilterChain(exclusive = false) {
    init {
        val allowedTopics = listOf(
            "27-british-columbia",
            "130-canadian-rockies",
            "34-alberta",
            "37-washington-state",
            "35-other-regions"
        )

        allowedTopics.forEach {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^/$it(?:|/|/index[0-9]{1,10}.html)$")))
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^/$it/[0-9]{1,7}[a-z0-9-]{1,250}[a-z](?<!print|prev-thread|next-thread).html$")))
        }
    }
}

object UBCVarsityOutdoorClubFilter: DocumentFilterChain(exclusive = true) {
    init {
        merge(DefaultFilterChain)
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/gallery/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/members/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/phorum5/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/wiki.*")))
    }
}

object DrDirtbagFilter: DocumentFilterChain(exclusive = true) {
    init {
        merge(DefaultFilterChain)
        addFilter(PathMatchingDocumentFilter(Pattern.compile(".*[0-9]{1,2}/$"))) // exclude directory style images
    }
}

object AmericanAlpineJournalIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/articles/[0-9]{1,20}.?$")))
    }
}

object AmericanAlpineJournalFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/articles/[0-9]{1,20}.?$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/articles\\?page=[0-9]{1,20}$")))
    }
}

object RockyMountainSummitsFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/.*/.*\\.htm$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/trip_reports/trip_report.php\\?trip_id=[0-9]{1,10}$")))
    }
}

object RockyMountainSummitsIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        merge(RockyMountainSummitsFilter)
    }
}

// Empty Filter, crawl anything
object IdahoClimbingGuideFilter: DocumentFilterChain(exclusive = true) {
    init {
        merge(WordPressFilterChain)
        merge(ImageFilterChain)
    }
}

object IdahoClimbingGuideIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/bookupdates/.*")))
    }
}