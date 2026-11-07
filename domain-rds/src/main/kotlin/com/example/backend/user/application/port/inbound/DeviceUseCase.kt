package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.UpdateDeviceCommand

/** 로그인과 무관한 기기 단위 등록과 광고성 수신 동의. */
interface DeviceUseCase {
    /** 보낸 필드만 반영하고 반영 후 동의 상태를 돌려준다. */
    fun update(command: UpdateDeviceCommand): DeviceConsent

    fun getConsent(deviceId: String): DeviceConsent
}
