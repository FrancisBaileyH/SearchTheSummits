package com.francisbailey.summitsearch.index.worker.extension

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import java.nio.charset.CodingErrorAction

suspend fun HttpResponse.bodyAsTextSafe(fallbackCharset: Charset = kotlin.text.Charsets.UTF_8): String {
    val originCharset = charset() ?: fallbackCharset
    val decoder = originCharset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
    val input = body<Input>()

    return decoder.decode(input)
}