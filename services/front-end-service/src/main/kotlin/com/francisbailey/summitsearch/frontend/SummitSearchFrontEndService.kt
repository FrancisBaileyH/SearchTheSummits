package com.francisbailey.summitsearch.frontend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ComponentScan("com.francisbailey.summitsearch")
@EnableScheduling
open class SummitSearchFrontEndService


fun main(args: Array<String>) {
    runApplication<SummitSearchFrontEndService>(*args)
}