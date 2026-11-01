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
            assertThat(meterRegistry.get("notification.record.dropped").counter().count()).isEqualTo(1.0)
            assertThat(rejectedTaskRan.get()).isFalse()
        } finally {
            release.countDown()
            executor.shutdown()
            meterRegistry.close()
        }
    }
}
