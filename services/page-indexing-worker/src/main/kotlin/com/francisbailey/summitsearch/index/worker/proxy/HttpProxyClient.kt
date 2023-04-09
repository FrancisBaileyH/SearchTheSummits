package com.francisbailey.summitsearch.index.worker.proxy

import java.net.URL

interface HttpProxyClient {
    fun <T> get(url: URL): T
}