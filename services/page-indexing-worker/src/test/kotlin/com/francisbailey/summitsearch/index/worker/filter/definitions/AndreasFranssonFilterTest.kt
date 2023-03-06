package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class AndreasFranssonFilterTest: FilterTest() {

    @Test
    fun `skips expected links`() {
        val expectedToSkip = listOf(
            "https://andreasfransson.se/blog/",
            "https://andreasfransson.se/blog",
            "https://andreasfransson.se/inner-adventures/"
        )

        val expectedNotToSkip = listOf(
            "https://andreasfransson.se/2013/11/the-new-zealand-adventure-the-mountain-tales-death-of-magnus-kastengren-from-my-perspective",
            "https://andreasfransson.se/2013/10/the-new-zealand-ski-adventure-mount-aspiring/"
        )

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
    }
}