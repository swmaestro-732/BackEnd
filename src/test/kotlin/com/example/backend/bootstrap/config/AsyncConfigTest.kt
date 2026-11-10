package com.example.backend.bootstrap.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AsyncConfigTest {
    @Test
    fun `운영시간 동기화 실행기는 큐가 차면 예외 없이 작업을 버린다`() {
        val executor = AsyncConfig().businessHoursSyncExecutor().apply { initialize() }
        val release = CountDownLatch(1)
        try {
            // 스레드 2개를 막고 큐를 가득 채운 뒤 한 건 더 넣는다.
            val blocking = Runnable { release.await(5, TimeUnit.SECONDS) }
            repeat(2 + AsyncConfig.BUSINESS_HOURS_SYNC_QUEUE_CAPACITY) { executor.execute(blocking) }

            assertDoesNotThrow { executor.execute { } }
            assertEquals(AsyncConfig.BUSINESS_HOURS_SYNC_QUEUE_CAPACITY, executor.threadPoolExecutor.queue.size)
        } finally {
            release.countDown()
            executor.shutdown()
        }
    }

    @Test
    fun `비동기 실행기는 요청 스레드의 관측을 작업 스레드로 넘긴다`() {
        val registry =
            ObservationRegistry.create().apply {
                observationConfig().observationHandler(ObservationHandler<Observation.Context> { true })
            }
        val accessor = ObservationThreadLocalAccessor.getInstance()
        val previousRegistry = accessor.observationRegistry
        accessor.observationRegistry = registry
        val executors =
            listOf(
                AsyncConfig().businessHoursSyncExecutor(),
                AsyncConfig().notificationExecutor(SimpleMeterRegistry()),
            ).onEach { it.initialize() }
        val parent = Observation.start("request", registry)
        try {
            parent.openScope().use {
                executors.forEach { executor ->
                    val seen = CompletableFuture<Observation?>()
                    executor.execute { seen.complete(registry.currentObservation) }
                    assertSame(parent, seen.get(5, TimeUnit.SECONDS))
                }
            }
        } finally {
            parent.stop()
            executors.forEach { it.shutdown() }
            accessor.observationRegistry = previousRegistry
        }
    }
}
