package com.francisbailey.summitsearch.index.worker.extension

import io.ktor.client.plugins.compression.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope

/**
 * For some reason S3 occasionally returns: Content-Encoding: UTF-8 in its headers
 * (probably customer configured). This is not a valid encoding and ktor ContentEncoder
 * fails. To get around this, I've created this dummy/passthrough encoder. If utf-8 content
 * encoding is encountered just process it as normal and move on!
 */
object Utf8PassThroughEncoder: ContentEncoder {
    /**
     * This value is stored case-insensitive on the ContentEncoder side
     */
    override val name = "UTF-8"
    override fun CoroutineScope.decode(source: ByteReadChannel) = source
    override fun CoroutineScope.encode(source: ByteReadChannel) = source
}