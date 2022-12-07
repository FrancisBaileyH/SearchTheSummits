package com.francisbailey.summitsearch.frontend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("com.francisbailey.summitsearch")
open class SummitSearchFrontEndService


fun main(args: Array<String>) {
    runApplication<SummitSearchFrontEndService>(*args)
}