package com.francisbailey.summitsearch.index.worker.filter.definitions

import org.junit.jupiter.api.Test

class DefaultFilterTest: FilterTest() {

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

        verifyFilter(documentIndexingFilterService, expectedToSkip, expectedNotToSkip)
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
            "/blog/wp-content/test",
            "/store/",
            "/product/abc123"
        ).map {
            urlString + it
        }

        val expectedNotToSkip = listOf(
            "/some-normal-page",
            "/2022/06/12/some-page",
            "/store-of-the-skies"
        ).map {
            urlString + it
        }

        verifyFilter(linkDiscoveryFilterService, expectedToSkip, expectedNotToSkip)
    }
}