package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.configuration.FilterConfiguration
import com.francisbailey.summitsearch.index.worker.configuration.SiteProcessingConfigurations
import com.francisbailey.summitsearch.index.worker.extension.normalizeAndEncode
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexFacebookPostStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.CascadeClimbersSubmitThumbnailStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerContentValidatorStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerSubmitThumbnailStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.SkiSicknessSubmitLinksStep
import org.junit.jupiter.api.Assertions
import org.mockito.kotlin.mock
import java.net.URL

abstract class FilterTest {

    private val indexFacebookPostStep = mock<IndexFacebookPostStep>()
    private val skiSicknessSubmitLinksStep = mock<SkiSicknessSubmitLinksStep>()
    private val peakBaggerSubmitThumbnailStep = mock<PeakBaggerSubmitThumbnailStep>()
    private val peakBaggerContentValidatorStep = mock<PeakBaggerContentValidatorStep>()
    private val cascadeClimbersSubmitThumbnailStep = mock<CascadeClimbersSubmitThumbnailStep>()

    private val indexSourceConfiguration = SiteProcessingConfigurations(
        indexFacebookPostStep,
        skiSicknessSubmitLinksStep,
        peakBaggerSubmitThumbnailStep,
        peakBaggerContentValidatorStep,
        cascadeClimbersSubmitThumbnailStep
    )

    private val configuration = FilterConfiguration(indexSourceConfiguration.indexingSourceOverrides())

    val linkDiscoveryFilterService = configuration.linkDiscoveryFilterService()

    val documentIndexingFilterService = configuration.documentIndexingFilterService()

    fun verifyFilter(service: DocumentFilterService, expectedToSkip: List<String>, expectedNotToSkip: List<String>) {
        expectedToSkip.forEach {
            Assertions.assertTrue(service.shouldFilter(URL(it).normalizeAndEncode()), "Failed on skipping: $it")
        }

        expectedNotToSkip.forEach {
            Assertions.assertFalse(service.shouldFilter(URL(it).normalizeAndEncode()), "Failed on not skipping: $it")
        }
    }

}