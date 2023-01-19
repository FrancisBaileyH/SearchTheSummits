package com.francisbailey.summitsearch.index.worker.extension

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.*
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

/**
 * Fixes bug where receiving body twice results in ParentJobCompleted exception
 * and allows a fallback to if decoding fails
 */
suspend fun HttpResponse.bodyAsTextWithFallback(fallbackCharset: Charset): String {
    val input = body<Input>()
    val originCharset = charset() ?: Charsets.UTF_8

    return try {
        originCharset.newDecoder().decode(input)
    } catch (e: MalformedInputException) {
        log.warn { "Failed to decode response from ${this.request.url}. Attempting to fallback with: $fallbackCharset" }
        fallbackCharset.newDecoder().decode(input)
    }
}

fun HttpStatusCode.isRedirect(): Boolean = when (value) {
    HttpStatusCode.MovedPermanently.value,
    HttpStatusCode.Found.value,
    HttpStatusCode.TemporaryRedirect.value,
    HttpStatusCode.PermanentRedirect.value,
    HttpStatusCode.SeeOther.value -> true
    else -> false
}