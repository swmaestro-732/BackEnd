package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/** `@Async` 활성화 — 커밋 후 외부 API 호출(운영시간 동기화)을 요청 스레드 밖에서 돌린다. 기기 활동 기록 실행기도 여기 둔다. */
@Configuration
@EnableAsync
class AsyncConfig {
    /**
     * 운영시간 동기화 전용 실행기. 작업마다 행 잠금과 DB 커넥션을 외부 호출 내내 쥐므로
     * 동시 실행을 [BUSINESS_HOURS_SYNC_THREADS] 로 묶어 Hikari 풀(10)의 대부분을 요청 경로에 남긴다.
     * Executor 빈이 생기면 Boot 의 기본 applicationTaskExecutor 는 자동 구성되지 않는다 — 현재 다른 사용처가 없어 괜찮다.
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
     * 큐가 차면 기본 AbortPolicy 로 거절하고, 호출부(DeviceActivityRecorder)가 받아 로그를 남기고 다음 요청에 다시 시도한다.
     */
    @Bean(DEVICE_ACTIVITY_EXECUTOR)
    fun deviceActivityExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 1
            queueCapacity = DEVICE_ACTIVITY_QUEUE_CAPACITY
            setThreadNamePrefix("device-activity-")
        }

    companion object {
        const val DEVICE_ACTIVITY_EXECUTOR = "deviceActivityExecutor"
        const val DEVICE_ACTIVITY_QUEUE_CAPACITY = 1_000
        const val BUSINESS_HOURS_SYNC_EXECUTOR = "businessHoursSyncExecutor"
        private const val BUSINESS_HOURS_SYNC_THREADS = 2
        const val BUSINESS_HOURS_SYNC_QUEUE_CAPACITY = 100
    }
}
