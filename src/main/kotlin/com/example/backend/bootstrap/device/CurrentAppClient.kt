package com.example.backend.bootstrap.device

/** 선택 앱 헤더(X-App-Platform, X-App-Build)에서 읽은 기기 정보. 헤더가 없으면 null. */
data class AppClient(
    val platform: String?,
    val build: Int?,
)

/**
 * [AppClient] 를 주입한다. 헤더는 선택이지만 값이 있는데 형식이 틀리면 INVALID_APP_HEADER.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentAppClient
