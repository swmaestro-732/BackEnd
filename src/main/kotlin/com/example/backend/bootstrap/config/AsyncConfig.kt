package com.example.backend.bootstrap.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 커밋 후 운영시간 동기화와 알림 기록을 각각 전용 실행기에서 처리한다.
 * 두 Executor 빈이 있어 Boot의 기본 applicationTaskExecutor는 자동 구성되지 않으므로 실행기를 명시한다.
 */
@Configuration
@EnableAsync
class AsyncConfig {
    private val log = KotlinLogging.logger {}

    /**
     * 운영시간 동기화 전용 실행기. 작업마다 행 잠금과 DB 커넥션을 외부 호출 내내 쥐므로
     * 동시 실행을 [BUSINESS_HOURS_SYNC_THREADS] 로 묶어 Hikari 풀(10)의 대부분을 요청 경로에 남긴다.
     * 큐는 [BUSINESS_HOURS_SYNC_QUEUE_CAPACITY] 로 묶고 넘치면 버린다. 동기화는 멱등이고 다음 상세 조회에서 다시 요청되며,
     * 기본 AbortPolicy 는 거절 예외를 커밋 후 콜백을 통해 요청 스레드로 던지기 때문이다(Google 장애 시 큐 무한 증가 방지).
     */
    @Bean(BUSINESS_HOURS_SYNC_EXECUTOR)
    fun businessHoursSyncExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = BUSINESS_HOURS_SYNC_THREADS
            maxPoolSize = BUSINESS_HOURS_SYNC_THREADS
            queueCapacity = BUSINESS_HOURS_SYNC_QUEUE_CAPACITY
            setRejectedExecutionHandler(ThreadPoolExecutor.DiscardPolicy())
            setThreadNamePrefix("business-hours-sync-")
        }

    @Bean(NOTIFICATION_EXECUTOR)
    fun notificationExecutor(meterRegistry: MeterRegistry): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 2
            queueCapacity = 1000
            setThreadNamePrefix("notification-")
            // 정상 종료(롤링 배포) 때 큐에 남은 기록을 마저 처리해 유실을 줄인다.
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            // AFTER_COMMIT 콜백의 거부 예외가 이미 커밋된 요청을 실패로 바꾸지 않도록 폐기한다.
            setRejectedExecutionHandler { _, _ ->
                log.debug { "알림 실행기 작업 폐기: 큐 포화 또는 실행기 종료" }
                meterRegistry.counter("notification.record.dropped").increment()
            }
        }

    companion object {
        const val NOTIFICATION_EXECUTOR = "notificationExecutor"
        const val BUSINESS_HOURS_SYNC_EXECUTOR = "businessHoursSyncExecutor"
        private const val BUSINESS_HOURS_SYNC_THREADS = 2
        const val BUSINESS_HOURS_SYNC_QUEUE_CAPACITY = 100
    }
}
