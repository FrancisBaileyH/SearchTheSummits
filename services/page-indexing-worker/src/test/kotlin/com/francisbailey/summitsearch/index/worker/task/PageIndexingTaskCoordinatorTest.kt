package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueuePollingClient
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.concurrent.Executor

class PageIndexingTaskCoordinatorTest {

    private val queueAssignmentStore = mock<QueueAssignmentStore>()
    private val executor = mock<Executor>()
    private val indexingTaskQueuePollingClient = mock<IndexingTaskQueuePollingClient>()
    private val searchIndexService = mock<SummitSearchIndexService>()
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val taskDependenciesCircuitBreaker = mock<CircuitBreaker>()
    private val perQueueCircuitBreaker = mock<CircuitBreaker>()

    private val circuitBreakerRegistry= mock<CircuitBreakerRegistry> {
        on(mock.circuitBreaker(PageIndexingTaskCoordinator.DEPENDENCY_CB_KEY)).thenReturn(taskDependenciesCircuitBreaker)
    }
    private val rateLimiterRegistry  = mock<RateLimiterRegistry>()
    private val taskPermitService = mock<TaskPermitService>()

    private val taskPermit = mock<TaskPermit>()


    private val indexingCoordinator = PageIndexingTaskCoordinator(
        queueAssignmentStore = queueAssignmentStore,
        indexingTaskExecutor = executor,
        taskRateLimiterRegistry = rateLimiterRegistry,
        circuitBreakerRegistry = circuitBreakerRegistry,
        indexingTaskQueuePollingClient = indexingTaskQueuePollingClient,
        summitSearchIndexService = searchIndexService,
        pageCrawlerService = pageCrawlerService,
        linkDiscoveryService = linkDiscoveryService,
        taskPermitService = taskPermitService
    )


    @Test
    fun `iterates through queues if there are any and adds task to executor`() {
        val queues = setOf("QueueA", "QueueB", "QueueC")

        whenever(queueAssignmentStore.getAssignments()).thenReturn(queues)
        whenever(perQueueCircuitBreaker.tryAcquirePermission()).thenReturn(true)
        whenever(taskDependenciesCircuitBreaker.tryAcquirePermission()).thenReturn(true)

        queues.forEach {
            whenever(circuitBreakerRegistry.circuitBreaker(it)).thenReturn(perQueueCircuitBreaker)
            whenever(taskPermitService.tryAcquirePermit(it)).thenReturn(taskPermit)
        }

        indexingCoordinator.coordinateTaskExecution()

        inOrder(circuitBreakerRegistry, perQueueCircuitBreaker, taskDependenciesCircuitBreaker, taskPermitService) {
            verify(circuitBreakerRegistry).circuitBreaker(PageIndexingTaskCoordinator.DEPENDENCY_CB_KEY)

            queues.forEach {
                verify(circuitBreakerRegistry).circuitBreaker(it)
                verify(taskDependenciesCircuitBreaker).tryAcquirePermission()
                verify(perQueueCircuitBreaker).tryAcquirePermission()
                verify(taskPermitService).tryAcquirePermit(it)
            }
        }

        verify(executor, times(queues.size)).execute(any<PageIndexingTask>())
    }

    @Test
    fun `does not execute tasks if there are no queues`() {
        val queues = emptySet<String>()
        whenever(queueAssignmentStore.getAssignments()).thenReturn(queues)

        indexingCoordinator.coordinateTaskExecution()

        verifyNoInteractions(executor)
    }

    @Test
    fun `does not execute any tasks if task level circuit breaker trips`() {
        val queues = setOf("QueueA", "QueueB", "QueueC")

        whenever(taskDependenciesCircuitBreaker.tryAcquirePermission()).thenReturn(false)
        whenever(queueAssignmentStore.getAssignments()).thenReturn(queues)

        indexingCoordinator.coordinateTaskExecution()

        verify(taskDependenciesCircuitBreaker, times(queues.size)).tryAcquirePermission()
        verifyNoInteractions(executor)
        verifyNoInteractions(taskPermitService)
    }

    @Test
    fun `skips any queues whose page level circuit breaker trips`() {
        val queues = setOf("QueueA", "QueueB", "QueueC")
        val trippedCircuitBreaker = mock<CircuitBreaker>()

        // Setup a tripped circuit breaker
        queues.forEach {
            whenever(circuitBreakerRegistry.circuitBreaker(it)).thenReturn(perQueueCircuitBreaker)
            whenever(taskPermitService.tryAcquirePermit(it)).thenReturn(taskPermit)
        }

        whenever(circuitBreakerRegistry.circuitBreaker("QueueB")).thenReturn(trippedCircuitBreaker)
        whenever(trippedCircuitBreaker.tryAcquirePermission()).thenReturn(false)
        whenever(taskDependenciesCircuitBreaker.tryAcquirePermission()).thenReturn(true)
        whenever(perQueueCircuitBreaker.tryAcquirePermission()).thenReturn(true)

        whenever(queueAssignmentStore.getAssignments()).thenReturn(queues)
        whenever(taskPermitService.tryAcquirePermit(any())).thenReturn(taskPermit)

        indexingCoordinator.coordinateTaskExecution()

        verify(taskDependenciesCircuitBreaker, times(queues.size)).tryAcquirePermission()

        inOrder(executor, taskDependenciesCircuitBreaker, circuitBreakerRegistry, perQueueCircuitBreaker, taskPermitService) {
            queues.filterNot { it == "QueueB" }.forEach {
                verify(circuitBreakerRegistry).circuitBreaker(it)
                verify(taskDependenciesCircuitBreaker).tryAcquirePermission()
                verify(perQueueCircuitBreaker).tryAcquirePermission()
                verify(taskPermitService).tryAcquirePermit(it)
                verify(executor).execute(any<PageIndexingTask>())
            }
        }

        verifyNoMoreInteractions(executor)
        verifyNoMoreInteractions(taskPermitService)
    }

    @Test
    fun `does not execute task if max permits have been issued already`() {
        val queues = setOf("QueueA", "QueueB", "QueueC")

        whenever(queueAssignmentStore.getAssignments()).thenReturn(queues)
        whenever(perQueueCircuitBreaker.tryAcquirePermission()).thenReturn(true)
        whenever(taskDependenciesCircuitBreaker.tryAcquirePermission()).thenReturn(true)

        queues.forEach {
            whenever(circuitBreakerRegistry.circuitBreaker(it)).thenReturn(perQueueCircuitBreaker)
            whenever(taskPermitService.tryAcquirePermit(it)).thenReturn(taskPermit)
        }

        whenever(taskPermitService.tryAcquirePermit("QueueB")).thenReturn(null)

        indexingCoordinator.coordinateTaskExecution()

        verify(taskDependenciesCircuitBreaker, times(queues.size)).tryAcquirePermission()

        inOrder(circuitBreakerRegistry, perQueueCircuitBreaker, taskDependenciesCircuitBreaker, taskPermitService, executor) {
            queues.filterNot { it == "QueueB" }.forEach {
                verify(circuitBreakerRegistry).circuitBreaker(it)
                verify(taskDependenciesCircuitBreaker).tryAcquirePermission()
                verify(perQueueCircuitBreaker).tryAcquirePermission()
                verify(taskPermitService).tryAcquirePermit(it)
                verify(executor).execute(any<PageIndexingTask>())
            }
        }

        verifyNoMoreInteractions(executor)
    }
}