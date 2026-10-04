package com.example.backend.media.application

/**
 * MediaService 가 필요로 하는 미디어 정책 값(업로드 상한, CDN base URL).
 * 애플리케이션 계층을 Spring Boot 설정 바인딩(@ConfigurationProperties)에서 떼어내기 위한 순수 계약이다.
 * 실제 값은 adapter 의 MediaProperties(@ConfigurationProperties)가 구현해 주입한다.
 */
interface MediaPolicy {
    val maxUploadBytes: Long
    val cdnBaseUrl: String
}
