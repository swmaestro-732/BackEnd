package com.example.backend.bootstrap.appversion

/**
 * `X-Device-Id` 헤더(기기 식별자, Android 는 Firebase Installation ID)를 주입한다.
 * 클라이언트가 바꿀 수 있는 값이라 인증, 권한 판단에 쓰지 않고 로그인 사용자와 묶어서만 쓴다.
 * 헤더가 없거나 비었거나 128자를 넘으면 INVALID_INPUT.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentDeviceId
