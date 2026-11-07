package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** `@Scheduled` 활성화. 현재 사용처는 알림 보관 기간 정리뿐이다. */
@Configuration
@EnableScheduling
class SchedulingConfig
