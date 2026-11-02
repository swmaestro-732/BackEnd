package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/** `@Async` 활성화. 실행기는 쓰는 쪽이 소유한다(운영시간 동기화: place 의 BusinessHoursSyncExecutorConfig). */
@Configuration
@EnableAsync
class AsyncConfig
