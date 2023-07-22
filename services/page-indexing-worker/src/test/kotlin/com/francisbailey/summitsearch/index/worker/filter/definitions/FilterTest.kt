package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.configuration.FilterConfiguration
import com.francisbailey.summitsearch.index.worker.configuration.SiteProcessingConfigurations
import com.francisbailey.summitsearch.index.worker.extension.normalizeAndEncode
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import org.junit.jupiter.api.Assertions
import java.net.URL

abstract class FilterTest {

    private val indexSourceConfiguration = SiteProcessingConfigurations()

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