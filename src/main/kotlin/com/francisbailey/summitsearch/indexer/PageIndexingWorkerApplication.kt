package com.francisbailey.summitsearch.indexer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling


@EnableScheduling
@SpringBootApplication
open class PageIndexingWorkerApplication

fun main(args: Array<String>) {
    runApplication<PageIndexingWorkerApplication>(*args)
}