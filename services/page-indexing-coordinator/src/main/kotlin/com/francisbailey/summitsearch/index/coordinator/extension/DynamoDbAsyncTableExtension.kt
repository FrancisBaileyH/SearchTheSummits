package com.francisbailey.summitsearch.index.coordinator.extension

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import java.util.concurrent.ExecutionException


fun <T> DynamoDbAsyncTable<T>.exists(): Boolean {
    return try {
        this.describeTable().get()
        true
    } catch (e: ExecutionException) {
        if (e.cause !is ResourceNotFoundException) {
            throw e
        }
        false
    }
}