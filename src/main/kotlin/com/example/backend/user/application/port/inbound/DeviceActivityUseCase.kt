package com.example.backend.user.application.port.inbound

/** 인바운드 포트: 리텐션 측정용 기기 일별 활동 기록(SCRUM-783). 요청 스레드를 막지 않고 비동기로 쓴다. */
interface DeviceActivityUseCase {
    /** [platform] 은 `android`, `ios` 처럼 정규화된 값이거나, 헤더가 없거나 알 수 없으면 null. */
    fun record(
        deviceId: String,
        userId: Long?,
        platform: String?,
    )
}
