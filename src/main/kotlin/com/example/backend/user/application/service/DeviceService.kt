package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.inbound.DeviceUseCase
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.inbound.dto.UpdateDeviceCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class DeviceService(
    private val devicePersistencePort: DevicePersistencePort,
    private val clock: Clock,
) : DeviceUseCase {
    override fun update(command: UpdateDeviceCommand): DeviceConsent {
        val changesConsent = command.marketing != null || command.night != null
        // 동의를 바꾸는 요청은 쓰기 전에 행을 잠그고 권한부터 확인한다. 토큰을 먼저 저장하면 확인을 우회할 수 있다.
        // 인증 없는 경로라, 토큰이 이미 있는 기기의 동의는 그 토큰(실제 설치만 가진 값)을 함께 보내야 바꿀 수 있다.
        val locked = if (changesConsent) devicePersistencePort.lockConsent(command.deviceId) else null
        if (locked?.fcmToken != null && locked.fcmToken != command.fcmToken) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "동의 변경에는 이 기기의 현재 토큰이 필요합니다.")
        }
        command.fcmToken?.let {
            // 비로그인 등록은 토큰이 같을 때만 기존 계정 연결을 유지한다(바뀌면 저장소가 연결을 끊는다).
            devicePersistencePort.saveToken(
                RegisterDeviceTokenCommand(command.deviceId, it, command.platform, command.appBuild),
                null,
            )
        }
        if (locked == null) {
            return devicePersistencePort.findConsent(command.deviceId) ?: DeviceConsent.NONE
        }
        val current = locked.consent
        val now = clock.instant()
        // 이미 동의한 상태에서 다시 동의하면 처음 동의 시각을 유지한다(2년 재확인 기준).
        val marketingAgreedAt =
            when (command.marketing) {
                true -> current.marketingAgreedAt ?: now
                false -> null
                null -> current.marketingAgreedAt
            }
        if (command.night == true && marketingAgreedAt == null) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "야간 수신 동의는 광고성 정보 수신 동의가 필요합니다.")
        }
        // 광고 동의를 철회하면 야간 동의도 함께 철회한다.
        val nightAgreedAt =
            when {
                marketingAgreedAt == null -> null
                command.night == true -> current.nightAgreedAt ?: now
                command.night == false -> null
                else -> current.nightAgreedAt
            }
        devicePersistencePort.updateConsent(command.deviceId, marketingAgreedAt, nightAgreedAt)
        return DeviceConsent(marketingAgreedAt, nightAgreedAt)
    }

    @Transactional(readOnly = true)
    override fun getConsent(deviceId: String): DeviceConsent =
        devicePersistencePort.findConsent(deviceId) ?: DeviceConsent.NONE
}
