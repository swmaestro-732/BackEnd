package com.example.backend.bootstrap.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 커밋 후 운영시간 동기화, 알림 기록, 검색 색인 카운터 갱신, 기기 활동 기록을 각각 전용 실행기에서 처리한다.
 * Executor 빈이 여럿이라 Boot의 기본 applicationTaskExecutor는 자동 구성되지 않으므로 `@Async` 에 실행기를 명시한다.
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

    /**
     * 기기 일별 활동 기록 전용 실행기. DB 커넥션 풀이 고갈돼도 요청 스레드가 커넥션을 기다리지 않게 기록을 밖으로 뺀다.
     * 큐가 차면 기본 AbortPolicy 로 거절하고, 호출부(DeviceActivityService)가 받아 로그를 남기고 다음 요청에 다시 시도한다.
     */
    @Bean(DEVICE_ACTIVITY_EXECUTOR)
    fun deviceActivityExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 1
            queueCapacity = DEVICE_ACTIVITY_QUEUE_CAPACITY
            // 배포 때 큐에 남은 기록을 잃지 않게 잠깐 기다린다.
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            setThreadNamePrefix("device-activity-")
        }

    @Bean(NOTIFICATION_EXECUTOR)
    fun notificationExecutor(meterRegistry: MeterRegistry): ThreadPoolTaskExecutor {
        // 원인별(큐 포화, 실행기 종료) 카운터를 0 으로 미리 등록해 첫 폐기부터 increase() 로 잡히게 한다.
        val droppedCounters =
            listOf(DROP_QUEUE_FULL, DROP_SHUTDOWN).associateWith {
                meterRegistry.counter("notification.record.dropped", "reason", it)
            }
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 2
            queueCapacity = 1000
            setThreadNamePrefix("notification-")
            // 정상 종료(롤링 배포) 때 큐에 남은 기록을 마저 처리해 유실을 줄인다.
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            // AFTER_COMMIT 콜백의 거부 예외가 이미 커밋된 요청을 실패로 바꾸지 않도록 폐기한다.
            setRejectedExecutionHandler { _, executor ->
                val reason = if (executor.isShutdown) DROP_SHUTDOWN else DROP_QUEUE_FULL
                log.debug { "알림 실행기 작업 폐기: reason=$reason" }
                droppedCounters.getValue(reason).increment()
            }
        }
    }

    /**
     * 검색 색인 카운터(저장수·좋아요수) 델타 전용 실행기 — 저장·좋아요 응답 경로에서 OpenSearch 왕복을 떼어낸다.
     * 델타는 가환이라 병렬로 돌아도 결과가 같고, 같은 문서 충돌은 스크립트의 retry_on_conflict 가 흡수한다. DB 커넥션은 쓰지 않는다.
     * 큐가 넘치면 델타를 버리고 warn 만 남긴다 — 다음 전체 색인(코스 편집·재색인)에서 DB 값으로 복구되고,
     * 기본 AbortPolicy 는 거절 예외를 커밋 후 콜백으로 이미 커밋된 요청에 되던지기 때문이다.
     */
    @Bean(SEARCH_INDEX_COUNTER_EXECUTOR)
    fun searchIndexCounterExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = SEARCH_INDEX_COUNTER_THREADS
            maxPoolSize = SEARCH_INDEX_COUNTER_THREADS
            queueCapacity = SEARCH_INDEX_COUNTER_QUEUE_CAPACITY
            setThreadNamePrefix("search-index-counter-")
            // 정상 종료(롤링 배포) 때 큐에 남은 델타를 마저 반영해 유실을 줄인다.
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            setRejectedExecutionHandler { _, executor ->
                log.warn {
                    "검색 색인 카운터 갱신 폐기(다음 전체 색인에서 복구): reason=${if (executor.isShutdown) DROP_SHUTDOWN else DROP_QUEUE_FULL}"
                }
            }
        }

    companion object {
        const val DEVICE_ACTIVITY_EXECUTOR = "deviceActivityExecutor"
        const val DEVICE_ACTIVITY_QUEUE_CAPACITY = 1_000
        const val NOTIFICATION_EXECUTOR = "notificationExecutor"
        const val DROP_QUEUE_FULL = "queue_full"
        const val DROP_SHUTDOWN = "shutdown"
        const val BUSINESS_HOURS_SYNC_EXECUTOR = "businessHoursSyncExecutor"
        private const val BUSINESS_HOURS_SYNC_THREADS = 2
        const val BUSINESS_HOURS_SYNC_QUEUE_CAPACITY = 100
        const val SEARCH_INDEX_COUNTER_EXECUTOR = "searchIndexCounterExecutor"
        private const val SEARCH_INDEX_COUNTER_THREADS = 2
        const val SEARCH_INDEX_COUNTER_QUEUE_CAPACITY = 1000
    }
}
