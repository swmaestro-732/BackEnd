package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** @Scheduled 활성화(코스 개수 SQS 폴러 등). 폴러 자체는 큐 설정 시에만 생성되므로 로컬·CI 에선 도는 작업이 없다. */
@Configuration
@EnableScheduling
class SchedulingConfig
