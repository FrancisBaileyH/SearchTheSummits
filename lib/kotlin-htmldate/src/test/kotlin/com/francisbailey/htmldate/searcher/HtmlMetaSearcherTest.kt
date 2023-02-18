package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class HtmlMetaSearcherTest {

    private val configuration = mock<HtmlDateSearchConfiguration> {
        on(mock.useOriginalDate).thenReturn(true)
    }

    private val parser = mock<HtmlDateParser>()

    private val searcher = HtmlMetaSearcher(configuration, parser)

    private val date = LocalDateTimeBuilder.of(2023)

    @Test
    fun `skips processing if content and datetime are missing`() {
        val document = buildDocumentWithHeader(listOf(
            "<meta name=\"some value\">"
        ))

        assertNull(searcher.getDateValue(document))
    }

    @Test
    fun `parses date if property is a date modified attribute and original date is false`() {
        val datePrefix = "some-date-"

        whenever(configuration.useOriginalDate).thenReturn(false)

        HtmlDateAttributes.PROPERTY_MODIFIED_ATTRIBUTES.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta property=\"$value\" content=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }

    @Test
    fun `does not parse date if property is a date modified attribute and original date is true`() {
        val datePrefix = "some-date-"

        whenever(parser.parse(any())).thenReturn(date)

        val metaHeaders = HtmlDateAttributes.PROPERTY_MODIFIED_ATTRIBUTES.mapIndexed { index, value ->
            val metaDate = "$datePrefix-$index"
            "<meta property=\"$value\" content=\"$metaDate\">"
        }

        val document = buildDocumentWithHeader(metaHeaders)

        assertNull(searcher.getDateValue(document))
    }

    @Test
    fun `parses any date attribute values with property`() {
        val datePrefix = "some-date-"

        HtmlDateAttributes.DATE_ATTRIBUTES.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta property=\"$value\" content=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }

    @Test
    fun `parses name attributes if they are a date attribute`() {
        val datePrefix = "some-date-"

        HtmlDateAttributes.DATE_ATTRIBUTES.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta name=\"$value\" content=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }

    @Test
    fun `parses name attributes if they are a modified attribute and use original date is false`() {
        val datePrefix = "some-date-"

        whenever(configuration.useOriginalDate).thenReturn(false)

        HtmlDateAttributes.MODIFIED_ATTRIBUTE_KEYS.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta name=\"$value\" content=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }

    @Test
    fun `does not parse name attributes if they are a modified attribute and use original date is true`() {
        val datePrefix = "some-date-"

        whenever(parser.parse("$datePrefix-${HtmlDateAttributes.MODIFIED_ATTRIBUTE_KEYS.size.dec()}")).thenReturn(date)

        val metaHeaders = HtmlDateAttributes.MODIFIED_ATTRIBUTE_KEYS.mapIndexed { index, value ->
            val metaDate = "$datePrefix-$index"
            "<meta name=\"$value\" content=\"$metaDate\">"
        }

        val document = buildDocumentWithHeader(metaHeaders)
        assertNull(searcher.getDateValue(document))
    }


    @Test
    fun `parses pubdate if its present`() {
        val metaDate = "some-date"

        whenever(parser.parse(metaDate)).thenReturn(date)

        val metaHeaders = listOf(
            "<meta pubdate=\"pubdate\" content=\"$metaDate\">"
        )

        val document = buildDocumentWithHeader(metaHeaders)
        val result = searcher.getDateValue(document)

        assertEquals(date, result)
    }

    @Test
    fun `skips item prop if its a modified value and use original date is true`() {
        val datePrefix = "some-date-"

        whenever(parser.parse("$datePrefix-${HtmlDateAttributes.ITEM_PROPERTY_MODIFIED.size.dec()}")).thenReturn(date)

        val metaHeaders = HtmlDateAttributes.ITEM_PROPERTY_MODIFIED.mapIndexed { index, value ->
            val metaDate = "$datePrefix-$index"
            "<meta itemprop=\"$value\" content=\"$metaDate\">"
        }

        val document = buildDocumentWithHeader(metaHeaders)
        assertNull(searcher.getDateValue(document))
    }

    @Test
    fun `skips item prop if its an original date value and use original date is false`() {
        val datePrefix = "some-date-"

        whenever(configuration.useOriginalDate).thenReturn(false)
        whenever(parser.parse("$datePrefix-${HtmlDateAttributes.ITEM_PROPERTY_ORIGINAL.size.dec()}")).thenReturn(date)

        val metaHeaders = HtmlDateAttributes.ITEM_PROPERTY_ORIGINAL.mapIndexed { index, value ->
            val metaDate = "$datePrefix-$index"
            "<meta itemprop=\"$value\" content=\"$metaDate\">"
        }

        val document = buildDocumentWithHeader(metaHeaders)
        assertNull(searcher.getDateValue(document))
    }

    @Test
    fun `parses item prop datetime if its present and use original date is true`() {
        val datePrefix = "some-date-"

        HtmlDateAttributes.ITEM_PROPERTY_ORIGINAL.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta itemprop=\"$value\" datetime=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }

    @Test
    fun `parses item prop content if its present and use original date is true`() {
        val datePrefix = "some-date-"

        HtmlDateAttributes.ITEM_PROPERTY_ORIGINAL.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta itemprop=\"$value\" content=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }

    @Test
    fun `parses item prop datetime if its present and use original date is true and is modified property`() {
        val datePrefix = "some-date-"

        whenever(configuration.useOriginalDate).thenReturn(false)

        HtmlDateAttributes.ITEM_PROPERTY_MODIFIED.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta itemprop=\"$value\" datetime=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }

    @Test
    fun `parses item prop content if its present and use original date is true and is modified property`() {
        val datePrefix = "some-date-"

        whenever(configuration.useOriginalDate).thenReturn(false)

        HtmlDateAttributes.ITEM_PROPERTY_MODIFIED.forEachIndexed {  index, value ->
            val metaDate = "$datePrefix-$index"
            whenever(parser.parse(metaDate)).thenReturn(date)

            val document = buildDocumentWithHeader(listOf("<meta itemprop=\"$value\" content=\"$metaDate\">"))
            val result = searcher.getDateValue(document)

            assertEquals(date, result)
        }
    }


    private fun buildDocumentWithHeader(metaValues: List<String>): Document {
        var html = "<html><head>"
        html += metaValues.joinToString(separator = " ")
        html += "</head><body></body>"

        return Jsoup.parse(html)
    }
}