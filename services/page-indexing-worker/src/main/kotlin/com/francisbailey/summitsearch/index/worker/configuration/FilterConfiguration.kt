package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.filter.definitions.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL

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
            addFilterChain(URL("https://www.mef.org.uk"), MountEverestFoundationFilter)
            addFilterChain(URL("https://www.nwhikers.net"), NWHikersFilter)
            addFilterChain(URL("https://peakbagger.com"), PeakBaggerFilter)
            addFilterChain(URL("http://www.alpinejournal.org.uk"), AlpineJournalUKFilter)
            addFilterChain(URL("https://alpinebenny.com"), AlpineBennyFilter)
            addFilterChain(URL("http://www.alpenglow.org"), AlpenGlowFilter)
            addFilterChain(URL("https://altusmountainguides.com"), AltusMountainGuidesFilter)
        }
    }

    @Bean
    open fun documentIndexingFilterService(): DocumentFilterService {
        return DocumentFilterService(defaultChain = DefaultIndexFilterChain).apply {
            addFilterChain(URL("http://sverdina.com"), SVerdinaIndexFilterChain)
            addFilterChain(URL("https://publications.americanalpineclub.org"), AmericanAlpineJournalIndexFilter)
            addFilterChain(URL("https://rockymountainsummits.com"), RockyMountainSummitsIndexFilter)
            addFilterChain(URL("https://www.idahoaclimbingguide.com"), IdahoClimbingGuideIndexFilter)
            addFilterChain(URL("https://www.mef.org.uk"), MountEverestFoundationIndexFilter)
            addFilterChain(URL("https://www.nwhikers.net"), NWHikersIndexFilter)
            addFilterChain(URL("https://peakbagger.com"), PeakBaggerIndexFilter)
            addFilterChain(URL("https://www.explor8ion.com"), Explor8ionIndexFilter)
            addFilterChain(URL("https://andreasfransson.se"), AndreasFranssonIndexFilter)
            addFilterChain(URL("https://andreasfransson.se"), AndreasFranssonIndexFilter)
        }
    }
}











