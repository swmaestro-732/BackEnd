package com.example.backend.bootstrap.config

import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

@Configuration
class ObservabilityConfig {
    @Bean
    fun actuatorObservationPredicate(): ObservationPredicate =
        ObservationPredicate { _, context ->
            // SCRUM-806: 헬스 체크와 스크레이프가 트레이스를 채우지 않도록 제외한다.
            context !is ServerRequestObservationContext || context.carrier?.requestURI?.startsWith("/actuator") != true
        }
}
