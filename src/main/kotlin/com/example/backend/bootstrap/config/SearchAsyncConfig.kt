package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 검색 색인 비동기 실행 전용 풀. 큐 초과 시 CallerRuns(호출 스레드에서 처리)로 유실 방지.
 */
@Configuration
@EnableAsync
class SearchAsyncConfig {
    @Bean
    fun searchIndexExecutor(): TaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 2
            queueCapacity = 100
            setThreadNamePrefix("search-index-")
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            initialize()
        }
}
