package com.example.backend.bootstrap.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class NotificationExecutorTest {
    @Test
    fun `알림 실행기는 작업자 두 개와 큐 천 개를 두고 포화 시 예외 없이 버린다`() {
        val meterRegistry = SimpleMeterRegistry()
        val executor = AsyncConfig().notificationExecutor(meterRegistry)
        val started = CountDownLatch(2)
        val release = CountDownLatch(1)
        val rejectedTaskRan = AtomicBoolean(false)
        executor.initialize()
        try {
            assertThat(meterRegistry.get("notification.record.dropped").counters().map { it.id.getTag("reason") })
                .containsExactlyInAnyOrder("queue_full", "shutdown")
            assertThat(meterRegistry.get("notification.record.dropped").counters().sumOf { it.count() }).isZero()
            assertThat(executor.corePoolSize).isEqualTo(2)
            assertThat(executor.maxPoolSize).isEqualTo(2)
            assertThat(executor.threadNamePrefix).isEqualTo("notification-")
            assertThat(executor.threadPoolExecutor.queue.remainingCapacity()).isEqualTo(1000)
            repeat(2) {
                executor.execute {
                    started.countDown()
                    release.await()
                }
            }
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
            repeat(1000) { executor.execute {} }

            assertDoesNotThrow { executor.execute { rejectedTaskRan.set(true) } }

            assertThat(executor.threadPoolExecutor.queue.size).isEqualTo(1000)
            assertThat(droppedCount(meterRegistry, "queue_full")).isEqualTo(1.0)
            assertThat(droppedCount(meterRegistry, "shutdown")).isZero()
            assertThat(rejectedTaskRan.get()).isFalse()
        } finally {
            release.countDown()
            executor.shutdown()
            meterRegistry.close()
        }
    }

    @Test
    fun `종료된 알림 실행기에 들어온 작업은 실행기 종료 원인으로 센다`() {
        val meterRegistry = SimpleMeterRegistry()
        val executor = AsyncConfig().notificationExecutor(meterRegistry)
        val rejectedTaskRan = AtomicBoolean(false)
        executor.initialize()
        executor.shutdown()

        assertDoesNotThrow { executor.execute { rejectedTaskRan.set(true) } }

        assertThat(droppedCount(meterRegistry, "shutdown")).isEqualTo(1.0)
        assertThat(droppedCount(meterRegistry, "queue_full")).isZero()
        assertThat(rejectedTaskRan.get()).isFalse()
        meterRegistry.close()
    }

    private fun droppedCount(
        meterRegistry: SimpleMeterRegistry,
        reason: String,
    ): Double =
        meterRegistry
            .get("notification.record.dropped")
            .tag("reason", reason)
            .counter()
            .count()
}
