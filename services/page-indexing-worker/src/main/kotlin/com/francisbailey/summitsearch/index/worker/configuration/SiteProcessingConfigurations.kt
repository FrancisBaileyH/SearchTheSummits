package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import com.francisbailey.summitsearch.index.worker.extractor.strategy.FacebookContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.definitions.*
import com.francisbailey.summitsearch.index.worker.indexing.StepOverride
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import com.francisbailey.summitsearch.index.worker.indexing.step.override.CascadeClimbersSubmitThumbnailStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerContentValidatorStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerSubmitThumbnailStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.SkiSicknessSubmitLinksStep
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL


data class SiteProcessingConfiguration(
    val source: URL,
    val discoveryFilter: DocumentFilterChain? = null,
    val indexingFilter: DocumentFilterChain? = null,
    val htmlContentSelector: ContentExtractorStrategy<DocumentText>? = null,
    val htmlProcessingOverrides: Set<StepOverride<DatedDocument>> = emptySet()
)


@Configuration
open class SiteProcessingConfigurations(
    indexFacebookPostStep: IndexFacebookPostStep,
    skiSicknessSubmitLinksStep: SkiSicknessSubmitLinksStep,
    peakBaggerSubmitThumbnailStep: PeakBaggerSubmitThumbnailStep,
    peakBaggerContentValidatorStep: PeakBaggerContentValidatorStep,
    cascadeClimbersSubmitThumbnailStep: CascadeClimbersSubmitThumbnailStep
) {

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
            htmlProcessingOverrides = setOf(
                StepOverride(
                    targetStep = SubmitThumbnailStep::class,
                    override = cascadeClimbersSubmitThumbnailStep
                )
            )
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
            discoveryFilter = ClubTreadFilter
        ),
        SiteProcessingConfiguration(
            source = URL("https://peakbagger.com"),
            discoveryFilter = PeakBaggerFilter,
            indexingFilter = PeakBaggerIndexFilter,
            htmlProcessingOverrides = setOf(
                StepOverride(
                    targetStep = ContentValidatorStep::class,
                    override = peakBaggerContentValidatorStep
                ),
                StepOverride(
                    targetStep = SubmitThumbnailStep::class,
                    override = peakBaggerSubmitThumbnailStep
                )
            )
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
            source = URL("https://skisickness.com"),
            discoveryFilter = SkiSicknessFilter,
            indexingFilter = SkiSicknessIndexFilter,
            htmlProcessingOverrides = setOf(
                StepOverride(
                    targetStep = SubmitLinksStep::class,
                    override = skiSicknessSubmitLinksStep
                )
            )
        ),
        SiteProcessingConfiguration(
            source = URL("http://sverdina.com"),
            indexingFilter = SVerdinaIndexFilterChain
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
            htmlProcessingOverrides = setOf(
                StepOverride(
                    targetStep = IndexHtmlPageStep::class,
                    override = indexFacebookPostStep
                )
            ),
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

    @Bean
    open fun htmlPipelineOverrides(): Map<URL, Set<StepOverride<DatedDocument>>> {
        return configurations.filterNot { it.htmlProcessingOverrides.isEmpty() }.associate {
            it.source to it.htmlProcessingOverrides
        }
    }
}