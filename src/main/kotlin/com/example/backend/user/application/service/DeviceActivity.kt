package com.example.backend.user.application.service

import java.time.LocalDate

/** 요청 하나가 남기려는 기기 활동(SCRUM-783). [DeviceActivityService] 안에서만 쓴다. */
internal data class DeviceActivity(
    val deviceId: String,
    val date: LocalDate,
    val userId: Long?,
    val platform: String?,
) {
    /**
     * 같은 날 같은 키는 한 번만 쓴다. 로그인 여부와 플랫폼 유무를 키에 넣어, 비로그인이나 플랫폼 없이 먼저 기록된 기기도
     * 그날 로그인하거나 플랫폼 헤더가 오면 한 번 더 써서 빈 user_id, platform 을 채운다(DB 는 NULL 일 때만 채움).
     */
    val key: Key get() = Key(deviceId, loggedIn = userId != null, hasPlatform = platform != null)

    data class Key(
        val deviceId: String,
        val loggedIn: Boolean,
        val hasPlatform: Boolean,
    )
}
