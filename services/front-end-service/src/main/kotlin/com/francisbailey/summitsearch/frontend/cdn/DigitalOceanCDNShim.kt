package com.francisbailey.summitsearch.frontend.cdn

import org.springframework.stereotype.Service
import java.net.URL

@Service
class DigitalOceanCDNShim {

    fun originToCDN(origin: URL): URL {
        val host = origin.host
        val components = host.split(".").toMutableList()

        components.add(REGION_INDEX, "cdn")

        return URL("${origin.protocol}://${components.joinToString(".")}${origin.path}")
    }


    companion object {
        const val REGION_INDEX = 2
    }

}