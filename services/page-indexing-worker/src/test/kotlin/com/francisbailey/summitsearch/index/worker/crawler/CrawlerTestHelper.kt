package com.francisbailey.summitsearch.index.worker.crawler

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.net.URL

/**
 * There's no great way to mock HttpResponse, so we'll just generate a real response with the
 * mock engine and test against that
 */
fun getResponse(url: URL, content: String, contentType: ContentType) = runBlocking {
    HttpClient(
        MockEngine {
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, contentType.toString())
            )
        }
    ).get(url)
}

fun getResponse(url: URL, content: ByteArray, contentType: ContentType) = runBlocking {
    HttpClient(
        MockEngine {
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, contentType.toString())
            )
        }
    ).get(url)
}