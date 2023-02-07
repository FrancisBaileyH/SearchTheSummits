package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.configuration.FilterConfiguration
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import org.junit.jupiter.api.Assertions
import java.net.URL

abstract class FilterTest {

    private val configuration = FilterConfiguration()

    val linkDiscoveryFilterService = configuration.linkDiscoveryFilterService()

    val documentIndexingFilterService = configuration.documentIndexingFilterService()

    fun verifyFilter(service: DocumentFilterService, expectedToSkip: List<String>, expectedNotToSkip: List<String>) {
        expectedToSkip.forEach {
            Assertions.assertTrue(service.shouldFilter(URL(it)))
        }

        expectedNotToSkip.forEach {
            Assertions.assertFalse(service.shouldFilter(URL(it)), "Failed on: $it")
        }
    }

}