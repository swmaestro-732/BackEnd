package com.example.backend.bootstrap.device

/**
 * 앱이 사용자 조작 없이 백그라운드에서 부르는 API(FCM 토큰 갱신 등)에 붙인다.
 * 붙은 요청은 리텐션 측정용 기기 일별 활동으로 세지 않는다(SCRUM-783). 클래스나 메서드에 붙일 수 있다.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotUserActivity
