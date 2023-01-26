package com.francisbailey.summitsearch.frontend.cdn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

class DigitalOceanCDNShimTest {


    private val shim = DigitalOceanCDNShim()


    @Test
    fun `generates DO cdn from origin url`() {
        val url = URL("https://sts-images.sfo3.digitaloceanspaces.com/www-francisbaileyh-com/5eecd32579f459ed61d90919af949f53ffbb0d94.png")
        val cdn = URL("https://sts-images.sfo3.cdn.digitaloceanspaces.com/www-francisbaileyh-com/5eecd32579f459ed61d90919af949f53ffbb0d94.png")

        val result = shim.originToCDN(url)

        assertEquals(cdn, result)
    }

}