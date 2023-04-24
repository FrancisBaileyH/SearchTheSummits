package com.francisbailey.summitsearch.index.worker.extractor

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.net.URL

class ContentExtractorTest {

    @Test
    fun `uses default extractor when no override found`() {
        val result = "TEST"
        val defaultExtractor = mock<ContentExtractorStrategy<String>>()

        whenever(defaultExtractor.extract(any())).thenReturn(result)

        val service = ContentExtractor(
            defaultExtractor,
            emptyMap()
        )

        assertEquals(result, service.extract(URL("https://some.com"), Jsoup.parse("")))
    }

    @Test
    fun `uses overridden extractor when override found`() {
        val result = "TEST"
        val defaultExtractor = mock<ContentExtractorStrategy<String>>()
        val override = mock<ContentExtractorStrategy<String>>()

        whenever(override.extract(any())).thenReturn(result)

        val service = ContentExtractor(
            defaultExtractor,
            mapOf(
                "some.com" to override
            )
        )

        assertEquals(result, service.extract(URL("https://some.com"), Jsoup.parse("")))
        verifyNoInteractions(defaultExtractor)
    }

}