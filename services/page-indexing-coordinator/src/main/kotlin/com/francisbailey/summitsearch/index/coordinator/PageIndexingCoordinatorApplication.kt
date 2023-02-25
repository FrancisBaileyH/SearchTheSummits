package com.francisbailey.summitsearch.index.coordinator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@ComponentScan("com.francisbailey.summitsearch")
open class PageIndexingCoordinatorApplication

fun main(args: Array<String>) {
    runApplication<PageIndexingCoordinatorApplication>(*args)
}