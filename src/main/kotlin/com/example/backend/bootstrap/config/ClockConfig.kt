package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** 시간 의존 로직(상대 시간 표기 등)의 테스트 가능성을 위해 Clock 을 빈으로 주입한다. 서버 시간은 UTC 통일. */
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
