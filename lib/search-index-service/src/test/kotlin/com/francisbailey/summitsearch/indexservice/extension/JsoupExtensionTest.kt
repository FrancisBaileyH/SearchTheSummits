package com.francisbailey.summitsearch.indexservice.extension

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JsoupExtensionTest {

    @Test
    fun `fetches seo description if one exists`() {
        val description = "Hello this is a test!"
        val html = """
            <html>  
                <head>
                    <meta name="description" content="$description">
                </head>
            </html>
        """

        assertEquals(description, Jsoup.parse(html).getSeoDescription())
    }

    @Test
    fun `returns null if no description exists()`() {
        val html = """
            <html>  
                <head>
                    <meta name="some-other-property" content="Test">
                </head>
            </html>
        """

        assertNull(Jsoup.parse(html).getSeoDescription())
    }

    @Test
    fun `strips html from description`() {
        val inputDescription = "Hello this <center>is</center> a test!"
        val expectedDescription = "Hello this is a test!"
        val html = """
            <html>  
                <head>
                    <meta name="description" content="$inputDescription">
                </head>
            </html>
        """

        assertEquals(expectedDescription, Jsoup.parse(html).getSeoDescription())
    }

}