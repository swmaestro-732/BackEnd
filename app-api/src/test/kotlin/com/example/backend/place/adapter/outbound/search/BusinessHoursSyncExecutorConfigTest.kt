package com.example.backend.place.adapter.outbound.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BusinessHoursSyncExecutorConfigTest {
    @Test
    fun `운영시간 동기화 실행기는 큐가 차면 예외 없이 작업을 버린다`() {
        val executor = BusinessHoursSyncExecutorConfig().businessHoursSyncExecutor().apply { initialize() }
        val release = CountDownLatch(1)
        try {
            // 스레드 2개를 막고 큐를 가득 채운 뒤 한 건 더 넣는다.
            val blocking = Runnable { release.await(5, TimeUnit.SECONDS) }
            repeat(
                2 + BusinessHoursSyncExecutorConfig.BUSINESS_HOURS_SYNC_QUEUE_CAPACITY,
            ) { executor.execute(blocking) }

            assertDoesNotThrow { executor.execute { } }
            assertEquals(
                BusinessHoursSyncExecutorConfig.BUSINESS_HOURS_SYNC_QUEUE_CAPACITY,
                executor.threadPoolExecutor.queue.size,
            )
        } finally {
            release.countDown()
            executor.shutdown()
        }
    }
}
