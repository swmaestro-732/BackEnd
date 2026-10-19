package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/** `@Async` 활성화 — 커밋 후 외부 API 호출(운영시간 동기화)을 요청 스레드 밖에서 돌린다. */
@Configuration
@EnableAsync
class AsyncConfig {
    /**
     * 운영시간 동기화 전용 실행기. 작업마다 행 잠금과 DB 커넥션을 외부 호출 내내 쥐므로
     * 동시 실행을 [BUSINESS_HOURS_SYNC_THREADS] 로 묶어 Hikari 풀(10)의 대부분을 요청 경로에 남긴다.
     * Executor 빈이 생기면 Boot 의 기본 applicationTaskExecutor 는 자동 구성되지 않는다 — 현재 다른 사용처가 없어 괜찮다.
     */
    @Bean(BUSINESS_HOURS_SYNC_EXECUTOR)
    fun businessHoursSyncExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = BUSINESS_HOURS_SYNC_THREADS
            maxPoolSize = BUSINESS_HOURS_SYNC_THREADS
            setThreadNamePrefix("business-hours-sync-")
        }

    companion object {
        const val BUSINESS_HOURS_SYNC_EXECUTOR = "businessHoursSyncExecutor"
        private const val BUSINESS_HOURS_SYNC_THREADS = 2
    }
}
