package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import com.francisbailey.summitsearch.index.worker.extractor.strategy.*
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.definitions.*
import com.francisbailey.summitsearch.index.worker.indexing.StepOverride
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import com.francisbailey.summitsearch.index.worker.indexing.step.override.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL


data class SiteProcessingConfiguration(
    val source: URL,
    val discoveryFilter: DocumentFilterChain? = null,
    val indexingFilter: DocumentFilterChain? = null,
    val htmlContentSelector: ContentExtractorStrategy<DocumentText>? = null,
    val imageContentSelector: ContentExtractorStrategy<List<CaptionedImage>>? = null,
)

@Configuration
open class SiteProcessingConfigurations {

   val configurations = setOf(
        SiteProcessingConfiguration(
            source = URL("https://accvi.ca"),
            discoveryFilter = ACCVIFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://alpinebenny.com"),
            discoveryFilter = AlpineBennyFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://altusmountainguides.com"),
            discoveryFilter = AltusMountainGuidesFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://amountainlifetime.net"),
            indexingFilter = AMountainLifetimeIndexFilter,
            imageContentSelector = AMountainLifeTimeImageExtractorStrategy()
        ),
        SiteProcessingConfiguration(
            source = URL("https://andreasfransson.se"),
            indexingFilter = AndreasFranssonIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://bcmc.ca"),
            discoveryFilter = BCMCFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://cascadeclimbers.com"),
            discoveryFilter = CascadeClimbersFilter,
        ),
        SiteProcessingConfiguration(
            source = URL("https://coastmountainguides.com"),
            discoveryFilter = CoastMountainGuidesFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://discourse.accvi.ca"),
            discoveryFilter = ACCVIDiscourseFilter,
            indexingFilter = ACCVIDiscourseIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://forums.clubtread.com"),
            discoveryFilter = ClubTreadFilter,
            htmlContentSelector = ClubTreadContentExtractorStrategy(),
            imageContentSelector = ClubTreadImageExtractorStrategy()
        ),
        SiteProcessingConfiguration(
            source = URL("https://pbase.com/nolock"),
            discoveryFilter = ScurlockFilter,
            indexingFilter = ScurlockIndexFilter,
            imageContentSelector = PBaseImageExtractorStrategy()
        ),
        SiteProcessingConfiguration(
            source = URL("https://peakbagger.com"),
            discoveryFilter = PeakBaggerFilter,
            indexingFilter = PeakBaggerIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://publications.americanalpineclub.org"),
            discoveryFilter = AmericanAlpineJournalFilter,
            indexingFilter = AmericanAlpineJournalIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://rockymountainsummits.com"),
            discoveryFilter = RockyMountainSummitsFilter,
            indexingFilter = RockyMountainSummitsIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://sites.google.com"),
            discoveryFilter = StephAbeggFilter,
            indexingFilter = StephAbeggIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://skisickness.com"),
            discoveryFilter = SkiSicknessFilter,
            indexingFilter = SkiSicknessIndexFilter,
        ),
        SiteProcessingConfiguration(
            source = URL("http://sverdina.com"),
            indexingFilter = SVerdinaIndexFilterChain
        ),
        SiteProcessingConfiguration(
            source = URL("https://wildairphoto.com"),
            discoveryFilter = WildAirFilter,
            htmlContentSelector = WildAirPhotographyContentExtractorStrategy()
        ),
       SiteProcessingConfiguration(
           source = URL("https://wildisle.ca"),
           discoveryFilter = WildIsleFilter
       ),
        SiteProcessingConfiguration(
            source = URL("http://www.alpenglow.org"),
            discoveryFilter = AlpenGlowFilter
        ),
        SiteProcessingConfiguration(
            source = URL("http://www.alpinejournal.org.uk"),
            discoveryFilter = AlpineJournalUKFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.drdirtbag.com"),
            discoveryFilter = DrDirtbagFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.explor8ion.com"),
            indexingFilter = Explor8ionIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://wwww.facebook.com"),
            htmlContentSelector = FacebookContentExtractorStrategy()
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.idahoaclimbingguide.com"),
            discoveryFilter = IdahoClimbingGuideFilter,
            indexingFilter = IdahoClimbingGuideIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.mef.org.uk"),
            discoveryFilter = MountEverestFoundationFilter,
            indexingFilter = MountEverestFoundationIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.mountaineers.org"),
            discoveryFilter = MountaineersOrgFilter,
            indexingFilter = MountaineersOrgIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.mountainproject.com"),
            htmlContentSelector = MountainProjectContentExtractorStrategy(),
            imageContentSelector = MountainProjectImageExtractorStrategy(),
            discoveryFilter = MountainProjectFilter,
            indexingFilter = MountainProjectIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.nwhikers.net"),
            discoveryFilter = NWHikersFilter,
            indexingFilter = NWHikersIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("http://www.supertopo.com"),
            discoveryFilter = SuperTopoFilter,
            indexingFilter = SuperTopoIndexFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://www.ubc-voc.com"),
            discoveryFilter = UBCVarsityOutdoorClubFilter
        )
    )

    @Bean
    open fun indexingSourceOverrides() = configurations
}

@Configuration
open class PipelineOverrideConfiguration(
    private val skiSicknessSubmitLinksStep: SkiSicknessSubmitLinksStep,
    private val peakBaggerSubmitThumbnailStep: PeakBaggerSubmitThumbnailStep,
    private val peakBaggerContentValidatorStep: PeakBaggerContentValidatorStep,
    private val mountainProjectContentValidatorStep: MountainProjectContentValidatorStep
) {

    @Bean
    open fun htmlPipelineOverrides(): Map<URL, Set<StepOverride<DatedDocument>>> {
        return mapOf(
            URL("https://peakbagger.com") to setOf(
                StepOverride(
                    targetStep = ContentValidatorStep::class,
                    override = peakBaggerContentValidatorStep
                ),
                StepOverride(
                    targetStep = SubmitThumbnailStep::class,
                    override = peakBaggerSubmitThumbnailStep
                )
            ),
            URL("https://skisickness.com") to setOf(StepOverride(
                targetStep = SubmitLinksStep::class,
                override = skiSicknessSubmitLinksStep
            )),
            URL("https://www.mountainproject.com") to setOf(StepOverride(
                targetStep = ContentValidatorStep::class,
                override = mountainProjectContentValidatorStep
            ))
        )
    }
}