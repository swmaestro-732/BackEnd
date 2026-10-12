package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/** `@Async` 활성화 — 커밋 후 외부 API 호출(운영시간 동기화)을 요청 스레드 밖에서 돌린다. 실행기는 Boot 기본 applicationTaskExecutor. */
@Configuration
@EnableAsync
class AsyncConfig
