package com.example.backend.bootstrap.config

import io.sentry.SentryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException

@Configuration
class SentryConfig {
    /**
     * 인증/인가 예외(401/403)는 정상 흐름이라 Sentry 에러로 보내지 않는다.
     *
     * 스타터의 SentryExceptionResolver 는 GlobalExceptionHandler 가 다시 던지는
     * AuthenticationException·AccessDeniedException 을 캡처할 수 있다(명시 캡처는 재던지기
     * 前이라 이미 제외). beforeSend 에서 원인 체인을 훑어 두 타입(및 하위)을 드롭 →
     * 인증/인가 실패는 이벤트 0개, 일반 예외의 500 만 기록한다.
     */
    @Bean
    fun sentryDropAuthErrors(): SentryOptions.BeforeSendCallback =
        SentryOptions.BeforeSendCallback { event, _ ->
            var t: Throwable? = event.throwable
            var depth = 0
            while (t != null && depth < 20) {
                if (t is AuthenticationException || t is AccessDeniedException) {
                    return@BeforeSendCallback null
                }
                t = t.cause
                depth++
            }
            event
        }
}
