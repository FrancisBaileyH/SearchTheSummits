package com.francisbailey.summitsearch.index.worker.extension

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

/**
 * Note that bodyAsText() only uses the fallback charset if there isn't a charset
 * supplied in the response header. For now, this is okay.
 */
suspend fun HttpResponse.bodyAsTextWithFallback(fallbackCharset: Charset): String {
    return try {
        this.bodyAsText()
    } catch (e: MalformedInputException) {
        log.warn { "Failed to decode response from ${this.request.url}. Attempting to fallback with: $fallbackCharset" }
        this.bodyAsText(fallbackCharset)
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