package com.francisbailey.summitsearch.index.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling


@EnableScheduling
@SpringBootApplication
@ComponentScan("com.francisbailey.summitsearch")
open class PageIndexingWorkerApplication

fun main(args: Array<String>) {
    runApplication<PageIndexingWorkerApplication>(*args)
}