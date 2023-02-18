package com.francisbailey.htmldate

import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import com.francisbailey.htmldate.extractor.DateExtractorStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

class HtmlDateParserTest {

    private val date = LocalDateTimeBuilder.of(2023)

    private val extractor1 = mock<DateExtractorStrategy>()
    private val extractor2 = mock<DateExtractorStrategy> {
        on(mock.find(any())).thenReturn(date)
    }

    private val parser = HtmlDateParser(
        listOf(extractor1, extractor2)
    )


    @Test
    fun `skips extractors if not enough digits`() {
        val value = "12345"
        assertNull(parser.parse(value))
    }

    @Test
    fun `skips extractors if not enough characters`() {
        val value = "abcde"
        assertNull(parser.parse(value))
    }

    @Test
    fun `skips extractors if date text probe fails`() {
        val value = "abc123456578"
        assertNull(parser.parse(value))
    }

    @Test
    fun `skips extractors if non date text probe fails`() {
        val value = "2022! Isn't that cool? 123456"
        assertNull(parser.parse(value))
    }

    @Test
    fun `runs parser when a regular date string is passed in`() {
        val value = "Today is January 22nd, 2022! Isn't that cool?"
        assertEquals(date, parser.parse(value))
    }




}