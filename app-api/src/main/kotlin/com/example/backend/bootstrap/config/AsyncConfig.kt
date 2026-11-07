package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * `@Async`, `@Scheduled` 활성화와 기본 실행기.
 * 운영시간 동기화 전용 실행기(place 의 BusinessHoursSyncExecutorConfig)가 있으면 Boot 가 기본 applicationTaskExecutor 를
 * 만들지 않으므로 여기서 직접 등록한다. 한정자 없는 `@Async` 와 `@ApplicationModuleListener` 가 이 실행기를 쓴다
 * (실행기가 둘 이상이면 Spring 은 taskExecutor 이름의 빈을 기본으로 고르므로 그 이름도 붙인다).
 */
@Configuration
@EnableAsync
@EnableScheduling
class AsyncConfig {
    @Bean(name = ["applicationTaskExecutor", "taskExecutor"])
    fun applicationTaskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 8
            queueCapacity = 500
            setThreadNamePrefix("app-async-")
        }
}
